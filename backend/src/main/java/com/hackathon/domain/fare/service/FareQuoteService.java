package com.hackathon.domain.fare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.entity.FareQuote;
import com.hackathon.domain.fare.repository.FareQuoteRepository;
import com.hackathon.global.client.OpenAiClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class FareQuoteService {

    private static final String AI_INSTRUCTIONS = """
            역할: 제공된 집계값으로 대한민국 화물 운임 기준을 계산하는 도우미다.

            목표: 입력값만 사용해 재현 가능한 평균 운임, 당일 저가 기준, 거리 값을 반환한다.

            계산 규칙:
            1. recentComparableCount가 1 이상이고 recentAverageFareKrw가 있으면 averageFare에 그 값을 그대로 사용한다. 아니면 baselineFareKrw를 사용한다.
            2. sameDayThreshold는 averageFare의 85%를 원 단위에서 반올림한 정수다.
            3. recentAverageDistanceKm가 있으면 distanceKm에 그 값을 그대로 사용하고, 없으면 null로 둔다.
            4. requestedWeightTon은 요청 문맥이다. 중량별 단가나 보정 규칙이 입력에 없으므로 이를 근거로 운임을 임의 조정하지 않는다.
            5. 실제 계약, 도로 거리, 실시간 시장 상황, 입력에 없는 할증을 추측하지 않는다.

            출력: 원화 값은 양의 정수이며 스키마에 없는 필드나 설명을 추가하지 않는다.
            """;
    private static final String AI_SCHEMA_JSON = """
            {
              "type":"object",
              "additionalProperties":false,
              "required":["averageFare","sameDayThreshold","distanceKm"],
              "properties":{
                "averageFare":{"type":"integer","minimum":10000,"maximum":10000000},
                "sameDayThreshold":{"type":"integer","minimum":10000,"maximum":10000000},
                "distanceKm":{"type":["integer","null"],"minimum":0,"maximum":2000}
              }
            }
            """;

    private final FareQuoteRepository fareQuoteRepository;
    private final CargoRepository cargoRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final JsonNode aiSchema;

    public FareQuoteService(FareQuoteRepository fareQuoteRepository, CargoRepository cargoRepository,
                            OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.fareQuoteRepository = fareQuoteRepository;
        this.cargoRepository = cargoRepository;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        try {
            this.aiSchema = objectMapper.readTree(AI_SCHEMA_JSON);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 시세 JSON Schema가 올바르지 않습니다.", e);
        }
    }

    /**
     * REQUIRES_NEW로 독립 트랜잭션을 연다 — CargoService.findDetail처럼 이미 트랜잭션이 열려있는
     * 호출자 안에서 예외가 나도(cache miss로 TODO 예외를 던지는 경우 등) 호출자의 트랜잭션까지
     * rollback-only로 오염시키지 않기 위함이다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FareQuoteResponse quote(String originSido, String destSido, CargoType cargoType,
                                   BigDecimal weightTon, Integer desiredFare) {
        String key = quoteKey(originSido, destSido, cargoType);
        FareQuote quote = fareQuoteRepository.findByQuoteKey(key)
                .orElseGet(() -> estimateAndCache(key, originSido, destSido, cargoType, weightTon));
        return FareQuoteResponse.of(quote, desiredFare);
    }

    /**
     * 캐시 미스에서만 AI로 시세를 추정하고 결과를 저장한다.
     * AI 장애·키 미설정·계약 위반 응답에는 등록 화물 평균 및 기본값으로 폴백한다.
     */
    private FareQuote estimateAndCache(String key, String originSido, String destSido, CargoType cargoType,
                                       BigDecimal weightTon) {
        List<Cargo> cargos = cargoRepository.findByOriginSidoAndDestSidoAndCargoType(
                originSido, destSido, cargoType);
        try {
            FareEstimate estimate = estimateWithAi(originSido, destSido, cargoType, weightTon, cargos);
            return fareQuoteRepository.save(FareQuote.create(key, estimate.averageFare(),
                    estimate.sameDayThreshold(), estimate.distanceKm()));
        } catch (Exception e) {
            log.warn("AI 시세 추정 실패 - 등록 화물 기반 값으로 폴백합니다. cargoType={}, sampleCount={}",
                    cargoType, cargos.size());
            return fareQuoteRepository.save(fallbackQuote(key, originSido, destSido, cargoType, cargos));
        }
    }

    private FareEstimate estimateWithAi(String originSido, String destSido, CargoType cargoType,
                                        BigDecimal weightTon, List<Cargo> cargos) throws JsonProcessingException {
        String output = openAiClient.generateStructured("fare_quote", AI_INSTRUCTIONS,
                aiInput(originSido, destSido, cargoType, weightTon, cargos), aiSchema);
        FareEstimate estimate = objectMapper.readValue(output, FareEstimate.class);
        FareEstimate expected = referenceEstimate(originSido, destSido, cargoType, cargos);
        if (estimate == null || estimate.averageFare() == null || estimate.sameDayThreshold() == null
                || estimate.averageFare() < 10_000 || estimate.averageFare() > 10_000_000
                || estimate.sameDayThreshold() < 10_000 || estimate.sameDayThreshold() > estimate.averageFare()
                || (estimate.distanceKm() != null && (estimate.distanceKm() < 0 || estimate.distanceKm() > 2_000))
                || !estimate.equals(expected)) {
            throw new IllegalArgumentException("AI 시세 추정 응답이 올바르지 않습니다.");
        }
        return estimate;
    }

    private String aiInput(String originSido, String destSido, CargoType cargoType,
                           BigDecimal weightTon, List<Cargo> cargos)
            throws JsonProcessingException {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("originSido", originSido);
        input.put("destSido", destSido);
        input.put("cargoType", cargoType.name());
        if (weightTon == null) {
            input.putNull("requestedWeightTon");
        } else {
            input.put("requestedWeightTon", weightTon);
        }
        input.put("recentComparableCount", cargos.size());
        putAverage(input, "recentAverageFareKrw", cargos.stream().map(Cargo::getDesiredFare)
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).average());
        putAverage(input, "recentAverageDistanceKm", cargos.stream().map(Cargo::getDistanceKm)
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).average());
        input.put("baselineFareKrw", fallbackFare(originSido, destSido, cargoType));
        return objectMapper.writeValueAsString(input);
    }

    private void putAverage(ObjectNode input, String field, java.util.OptionalDouble value) {
        if (value.isPresent()) {
            input.put(field, Math.round(value.getAsDouble()));
        } else {
            input.putNull(field);
        }
    }

    private FareQuote fallbackQuote(String key, String originSido, String destSido, CargoType cargoType,
                                    List<Cargo> cargos) {
        FareEstimate estimate = referenceEstimate(originSido, destSido, cargoType, cargos);
        return FareQuote.create(key, estimate.averageFare(), estimate.sameDayThreshold(), estimate.distanceKm());
    }

    private FareEstimate referenceEstimate(String originSido, String destSido, CargoType cargoType,
                                           List<Cargo> cargos) {
        int averageFare = cargos.stream().map(Cargo::getDesiredFare).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().mapToInt(value -> (int) Math.round(value))
                .findFirst().orElseGet(() -> fallbackFare(originSido, destSido, cargoType));
        Integer distanceKm = cargos.stream().map(Cargo::getDistanceKm).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().mapToInt(value -> (int) Math.round(value))
                .boxed().findFirst().orElse(null);
        int sameDayThreshold = BigDecimal.valueOf(averageFare).multiply(new BigDecimal("0.85"))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return new FareEstimate(averageFare, sameDayThreshold, distanceKm);
    }

    private int fallbackFare(String originSido, String destSido, CargoType cargoType) {
        int baseFare = originSido.equals(destSido) ? 180_000 : 600_000;
        return switch (cargoType) {
            case REFRIGERATED, FROZEN -> (int) Math.round(baseFare * 1.15);
            case HAZARDOUS -> (int) Math.round(baseFare * 1.2);
            default -> baseFare;
        };
    }

    private String quoteKey(String originSido, String destSido, CargoType cargoType) {
        return originSido + "|" + destSido + "|" + cargoType;
    }

    private record FareEstimate(Integer averageFare, Integer sameDayThreshold, Integer distanceKm) {
    }
}
