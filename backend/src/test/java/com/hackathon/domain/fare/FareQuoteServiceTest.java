package com.hackathon.domain.fare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.entity.FareQuote;
import com.hackathon.domain.fare.repository.FareQuoteRepository;
import com.hackathon.domain.fare.service.FareQuoteService;
import com.hackathon.global.client.OpenAiClient;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FareQuoteServiceTest {

    @Autowired
    FareQuoteService fareQuoteService;

    @Autowired
    FareQuoteRepository fareQuoteRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OpenAiClient openAiClient;

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

    @Test
    @DisplayName("캐시 미스에서는 AI 구조화 출력을 저장하고 다음 조회에 재사용한다")
    void cachesAiEstimatedQuote() throws Exception {
        when(openAiClient.generateStructured(eq("fare_quote"), any(), any(), any()))
                .thenReturn("""
                        {"averageFare":690000,"sameDayThreshold":586500,"distanceKm":null}
                        """);

        FareQuoteResponse first = fareQuoteService.quote(
                "울산광역시", "제주특별자치도", CargoType.FROZEN, new BigDecimal("5.0"), null);
        FareQuoteResponse second = fareQuoteService.quote(
                "울산광역시", "제주특별자치도", CargoType.FROZEN, new BigDecimal("5.0"), null);

        assertThat(first.averageFare()).isEqualTo(690_000);
        assertThat(second.distanceKm()).isNull();

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiClient).generateStructured(
                eq("fare_quote"), instructionsCaptor.capture(), inputCaptor.capture(), any());

        assertThat(instructionsCaptor.getValue())
                .contains("재현 가능한 평균 운임")
                .contains("baselineFareKrw")
                .contains("averageFare의 85%")
                .contains("requestedWeightTon은 요청 문맥")
                .contains("운임을 임의 조정하지 않는다");
        JsonNode input = objectMapper.readTree(inputCaptor.getValue());
        assertThat(input.at("/requestedWeightTon").decimalValue()).isEqualByComparingTo("5.0");
        assertThat(input.at("/baselineFareKrw").asInt()).isEqualTo(690_000);
    }

    @Test
    @DisplayName("AI 계산값이 명시된 운임 계약과 다르면 서버 기준값으로 폴백한다")
    void fallsBackWhenAiViolatesFareCalculationContract() {
        when(openAiClient.generateStructured(eq("fare_quote"), any(), any(), any()))
                .thenReturn("""
                        {"averageFare":700000,"sameDayThreshold":595000,"distanceKm":null}
                        """);

        FareQuoteResponse response = fareQuoteService.quote(
                "테스트출발지", "테스트도착지", CargoType.GENERAL, null, null);

        assertThat(response.averageFare()).isEqualTo(600_000);
        assertThat(response.sameDayThreshold()).isEqualTo(510_000);
    }
}
