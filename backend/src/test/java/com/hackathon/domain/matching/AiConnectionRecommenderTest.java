package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.matching.service.AiConnectionRecommender;
import com.hackathon.domain.matching.service.ConnectionRecommendationCandidate;
import com.hackathon.global.client.OpenAiClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiConnectionRecommenderTest {

    private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
    private final AiConnectionRecommender recommender =
            new AiConnectionRecommender(openAiClient, new ObjectMapper());

    @Test
    void selectsExactlyOneCandidateAndReturnsAiReasons() {
        when(openAiClient.generateStructured(
                eq("connection_recommendation"), any(), any(), any()))
                .thenReturn("""
                        {
                          "cargoId": 2,
                          "recommendationSummary": "공차 이동이 짧고 운임이 좋은 화물입니다.",
                          "reasons": ["공차 이동 거리가 8km입니다.", "추가 운임은 70만원입니다."]
                        }
                        """);

        AiConnectionRecommender.RecommendationDecision result = recommender.recommend(List.of(
                candidate(1L, 90, 600_000, 5.0, 120),
                candidate(2L, 85, 700_000, 8.0, 90)
        ));

        assertThat(result.cargoId()).isEqualTo(2L);
        assertThat(result.reasons()).hasSize(2);
        assertThat(result.explanationMode()).isEqualTo("AI");
    }

    @Test
    void fallsBackToRuleTopCandidateWhenAiSelectsUnknownCargo() {
        when(openAiClient.generateStructured(
                eq("connection_recommendation"), any(), any(), any()))
                .thenReturn("""
                        {
                          "cargoId": 999,
                          "recommendationSummary": "존재하지 않는 후보입니다.",
                          "reasons": ["이유 1", "이유 2"]
                        }
                        """);

        AiConnectionRecommender.RecommendationDecision result = recommender.recommend(List.of(
                candidate(1L, 90, 600_000, 5.0, 120),
                candidate(2L, 85, 700_000, 8.0, 90)
        ));

        assertThat(result.cargoId()).isEqualTo(1L);
        assertThat(result.reasons()).hasSize(3);
        assertThat(result.explanationMode()).isEqualTo("RULE_FALLBACK");
    }

    private ConnectionRecommendationCandidate candidate(
            Long cargoId,
            int score,
            int fare,
            double emptyDistanceKm,
            long waitMinutes
    ) {
        Cargo cargo = Mockito.mock(Cargo.class);
        when(cargo.getId()).thenReturn(cargoId);
        when(cargo.getOriginSido()).thenReturn("부산광역시");
        when(cargo.getOriginSigungu()).thenReturn("강서구");
        when(cargo.getDestSido()).thenReturn("서울특별시");
        when(cargo.getDestSigungu()).thenReturn("강남구");
        when(cargo.getLoadingAt()).thenReturn(LocalDateTime.of(2026, 8, 14, 18, 0));
        when(cargo.getCargoType()).thenReturn(CargoType.GENERAL);
        when(cargo.getWeightTon()).thenReturn(new BigDecimal("5.0"));
        when(cargo.getDesiredFare()).thenReturn(fare);
        return new ConnectionRecommendationCandidate(cargo, emptyDistanceKm, waitMinutes, score);
    }
}
