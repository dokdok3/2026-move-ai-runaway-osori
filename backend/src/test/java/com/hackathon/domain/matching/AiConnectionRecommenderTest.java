package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AiConnectionRecommenderTest {

    private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiConnectionRecommender recommender =
            new AiConnectionRecommender(openAiClient, objectMapper);

    @Test
    void selectsExactlyOneCandidateAndReturnsAiReasons() throws Exception {
        when(openAiClient.generateStructured(
                eq("connection_recommendation"), any(), any(), any()))
                .thenReturn("""
                        {
                          "cargoId": 1,
                          "recommendationSummary": "규칙 점수가 가장 높은 화물입니다.",
                          "reasons": ["규칙 점수는 90점입니다.", "공차 이동 거리는 5km입니다."]
                        }
                        """);

        AiConnectionRecommender.RecommendationDecision result = recommender.recommend(List.of(
                candidate(1L, 90, 600_000, 5.0, 120),
                candidate(2L, 85, 700_000, 8.0, 90)
        ));

        assertThat(result.cargoId()).isEqualTo(1L);
        assertThat(result.reasons()).hasSize(2);
        assertThat(result.explanationMode()).isEqualTo("AI");

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> schemaCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).generateStructured(eq("connection_recommendation"),
                instructionsCaptor.capture(), inputCaptor.capture(), schemaCaptor.capture());

        assertThat(instructionsCaptor.getValue())
                .contains("서버의 규칙 순위를 유지")
                .contains("ruleScore가 가장 높은 후보")
                .contains("별도 가중치를 만들거나 점수를 재계산하지 않는다")
                .contains("emptyDistanceKm가 짧은 후보, fareKrw가 높은 후보, cargoId가 작은 후보")
                .contains("정확히 1건만 선택");
        JsonNode input = objectMapper.readTree(inputCaptor.getValue());
        assertThat(input.at("/candidates/0/cargoId").asLong()).isEqualTo(1L);
        assertThat(input.at("/candidates/0/ruleScore").asInt()).isEqualTo(90);
        assertThat(input.at("/candidates/0/waitMinutes").asLong()).isEqualTo(120L);
        assertThat(schemaCaptor.getValue().at("/properties/recommendationSummary/minLength").asInt())
                .isEqualTo(1);
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

    @Test
    void fallsBackToRuleTopCandidateWhenAiChangesRuleOrder() {
        when(openAiClient.generateStructured(
                eq("connection_recommendation"), any(), any(), any()))
                .thenReturn("""
                        {
                          "cargoId": 2,
                          "recommendationSummary": "후순위 후보를 선택했습니다.",
                          "reasons": ["공차 이동 거리는 8km입니다.", "운임은 70만원입니다."]
                        }
                        """);

        AiConnectionRecommender.RecommendationDecision result = recommender.recommend(List.of(
                candidate(1L, 90, 600_000, 5.0, 120),
                candidate(2L, 85, 700_000, 8.0, 90)
        ));

        assertThat(result.cargoId()).isEqualTo(1L);
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
