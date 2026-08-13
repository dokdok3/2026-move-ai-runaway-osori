package com.hackathon.domain.fare.service;

import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.entity.FareQuote;
import com.hackathon.domain.fare.repository.FareQuoteRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FareQuoteService {

    private final FareQuoteRepository fareQuoteRepository;

    /**
     * REQUIRES_NEW로 독립 트랜잭션을 연다 — CargoService.findDetail처럼 이미 트랜잭션이 열려있는
     * 호출자 안에서 예외가 나도(cache miss로 TODO 예외를 던지는 경우 등) 호출자의 트랜잭션까지
     * rollback-only로 오염시키지 않기 위함이다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FareQuoteResponse quote(String originSido, String destSido, CargoType cargoType,
                                   BigDecimal weightTon, Integer desiredFare) {
        String key = quoteKey(originSido, destSido, cargoType, weightTon);
        FareQuote quote = fareQuoteRepository.findByQuoteKey(key)
                .orElseGet(() -> estimateAndCache(key));
        return FareQuoteResponse.of(quote, desiredFare);
    }

    /**
     * TODO(AI): Claude API(structured outputs)로 구간 평균 운임(averageFare)·당일 매칭 임계 운임
     * (sameDayThreshold)·거리(distanceKm)를 추정해 FareQuote로 저장한다.
     * - 임계는 평균의 80~90% 수준으로 프롬프트에 명시하되, 모델이 규칙을 어길 경우를 대비해
     *   코드에서도 Math.min(threshold, average)로 방어한다.
     * - 같은 quoteKey면 항상 같은 값을 돌려줘야 데모 재현성이 보장되므로, 이 메서드가
     *   캐시를 채우는 유일한 지점이어야 한다 (요청마다 새로 추정하지 않는다).
     * - global/ai/ClaudeClient(아직 없음)를 통해 호출할 것.
     */
    private FareQuote estimateAndCache(String key) {
        throw new UnsupportedOperationException(
                "TODO(AI): 구간 시세 추정이 아직 연동되지 않았습니다 (quoteKey=" + key + ")");
    }

    private String quoteKey(String originSido, String destSido, CargoType cargoType, BigDecimal weightTon) {
        return originSido + "|" + destSido + "|" + cargoType + "|" + weightTon;
    }
}
