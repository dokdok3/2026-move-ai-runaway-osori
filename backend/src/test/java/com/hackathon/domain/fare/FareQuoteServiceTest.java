package com.hackathon.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.entity.FareQuote;
import com.hackathon.domain.fare.repository.FareQuoteRepository;
import com.hackathon.domain.fare.service.FareQuoteService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FareQuoteServiceTest {

    @Autowired
    FareQuoteService fareQuoteService;

    @Autowired
    FareQuoteRepository fareQuoteRepository;

    @Test
    @DisplayName("캐시된 시세가 있으면 AI 호출 없이 verdict를 계산해 돌려준다")
    void returnsCachedQuoteWithVerdict() {
        fareQuoteRepository.save(FareQuote.create(
                "서울특별시|부산광역시|REFRIGERATED", 720_000, 620_000, 325));

        FareQuoteResponse slow = fareQuoteService.quote(
                "서울특별시", "부산광역시", CargoType.REFRIGERATED, new BigDecimal("5.0"), 500_000);
        assertThat(slow.verdict()).isEqualTo("SLOW");
        assertThat(slow.averageFare()).isEqualTo(720_000);
        assertThat(slow.message()).contains("낮아요");

        FareQuoteResponse fair = fareQuoteService.quote(
                "서울특별시", "부산광역시", CargoType.REFRIGERATED, new BigDecimal("5.0"), 700_000);
        assertThat(fair.verdict()).isEqualTo("FAIR");
        assertThat(fair.message()).isNull();
    }

    @Test
    @DisplayName("캐시가 없어도 등록 화물 또는 기본값으로 시세를 저장해 반환한다")
    void cachesFallbackQuoteWhenCacheMiss() {
        FareQuoteResponse response = fareQuoteService.quote(
                "제주특별자치도", "제주특별자치도", CargoType.GENERAL, null, 100_000);

        assertThat(response.averageFare()).isPositive();
        assertThat(response.sameDayThreshold()).isLessThanOrEqualTo(response.averageFare());
        assertThat(fareQuoteRepository.findByQuoteKey("제주특별자치도|제주특별자치도|GENERAL")).isPresent();
    }
}
