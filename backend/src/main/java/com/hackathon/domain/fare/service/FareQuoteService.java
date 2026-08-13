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
            당신은 대한민국 화물 운임의 보수적 추정 도우미다.
            제공된 구간·화물 유형·중량·최근 동일 조건 집계만 근거로 평균 운임을 추정한다.
            제공되지 않은 계약, 실제 도로 거리, 실시간 시장 데이터를 알고 있는 것처럼 주장하지 않는다.
            averageFare와 sameDayThreshold는 한국 원화 정수이며 양수여야 한다.
            sameDayThreshold는 averageFare보다 클 수 없다.
            distanceKm은 근거가 없으면 null을 반환한다.
            반드시 제공된 JSON Schema에 맞는 JSON만 반환한다.
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
                .orElseGet(() -> estimateAndCache(key, originSido, destSido, cargoType));
        return FareQuoteResponse.of(quote, desiredFare);
    }

    /**
     * 캐시 미스에서만 AI로 시세를 추정하고 결과를 저장한다.
     * AI 장애·키 미설정·계약 위반 응답에는 등록 화물 평균 및 기본값으로 폴백한다.
     */
    private FareQuote estimateAndCache(String key, String originSido, String destSido, CargoType cargoType) {
        List<Cargo> cargos = cargoRepository.findByOriginSidoAndDestSidoAndCargoType(
                originSido, destSido, cargoType);
        try {
            FareEstimate estimate = estimateWithAi(originSido, destSido, cargoType, cargos);
            return fareQuoteRepository.save(FareQuote.create(key, estimate.averageFare(),
                    estimate.sameDayThreshold(), estimate.distanceKm()));
        } catch (Exception e) {
            log.warn("AI 시세 추정 실패 - 등록 화물 기반 값으로 폴백합니다. cargoType={}, sampleCount={}",
                    cargoType, cargos.size());
            return fareQuoteRepository.save(fallbackQuote(key, originSido, destSido, cargoType, cargos));
        }
    }

    private FareEstimate estimateWithAi(String originSido, String destSido, CargoType cargoType,
                                        List<Cargo> cargos) throws JsonProcessingException {
        String output = openAiClient.generateStructured("fare_quote", AI_INSTRUCTIONS,
                aiInput(originSido, destSido, cargoType, cargos), aiSchema);
        FareEstimate estimate = objectMapper.readValue(output, FareEstimate.class);
        if (estimate == null || estimate.averageFare() == null || estimate.sameDayThreshold() == null
                || estimate.averageFare() < 10_000 || estimate.averageFare() > 10_000_000
                || estimate.sameDayThreshold() < 10_000 || estimate.sameDayThreshold() > estimate.averageFare()
                || (estimate.distanceKm() != null && (estimate.distanceKm() < 0 || estimate.distanceKm() > 2_000))) {
            throw new IllegalArgumentException("AI 시세 추정 응답이 올바르지 않습니다.");
        }
        return estimate;
    }

    private String aiInput(String originSido, String destSido, CargoType cargoType, List<Cargo> cargos)
            throws JsonProcessingException {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("originSido", originSido);
        input.put("destSido", destSido);
        input.put("cargoType", cargoType.name());
        input.put("recentComparableCount", cargos.size());
        putAverage(input, "recentAverageFareKrw", cargos.stream().map(Cargo::getDesiredFare)
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).average());
        putAverage(input, "recentAverageDistanceKm", cargos.stream().map(Cargo::getDistanceKm)
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).average());
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
        int averageFare = cargos.stream().map(Cargo::getDesiredFare).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().mapToInt(value -> (int) Math.round(value))
                .findFirst().orElseGet(() -> fallbackFare(originSido, destSido, cargoType));
        Integer distanceKm = cargos.stream().map(Cargo::getDistanceKm).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().mapToInt(value -> (int) Math.round(value))
                .boxed().findFirst().orElse(null);
        int sameDayThreshold = BigDecimal.valueOf(averageFare).multiply(new BigDecimal("0.85"))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return FareQuote.create(key, averageFare, sameDayThreshold, distanceKm);
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
