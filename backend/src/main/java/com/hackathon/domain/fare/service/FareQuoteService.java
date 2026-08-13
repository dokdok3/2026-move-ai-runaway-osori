package com.hackathon.domain.fare.service;

import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.entity.FareQuote;
import com.hackathon.domain.fare.repository.FareQuoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FareQuoteService {

    private final FareQuoteRepository fareQuoteRepository;
    private final CargoRepository cargoRepository;

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
     * 같은 권역·화물유형의 등록 화물을 우선 활용해 시세 캐시를 채운다.
     * 외부 AI 호출이 없으므로 캐시 miss가 API 장애나 토큰 비용으로 이어지지 않는다.
     */
    private FareQuote estimateAndCache(String key, String originSido, String destSido, CargoType cargoType) {
        List<Cargo> cargos = cargoRepository.findByOriginSidoAndDestSidoAndCargoType(
                originSido, destSido, cargoType);
        int averageFare = cargos.isEmpty()
                ? fallbackFare(originSido, destSido, cargoType)
                : cargos.stream().map(Cargo::getDesiredFare).filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue).average().stream()
                        .mapToInt(value -> (int) Math.round(value)).findFirst()
                        .orElseGet(() -> fallbackFare(originSido, destSido, cargoType));
        Integer distanceKm = cargos.stream().map(Cargo::getDistanceKm).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream()
                .mapToInt(value -> (int) Math.round(value)).boxed().findFirst().orElse(null);
        int sameDayThreshold = BigDecimal.valueOf(averageFare).multiply(new BigDecimal("0.85"))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return fareQuoteRepository.save(FareQuote.create(key, averageFare, sameDayThreshold, distanceKm));
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
}
