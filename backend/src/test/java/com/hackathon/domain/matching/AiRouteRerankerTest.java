package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.service.AiRouteReranker;
import com.hackathon.global.client.OpenAiClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AiRouteRerankerTest {

    private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiRouteReranker reranker = new AiRouteReranker(openAiClient, objectMapper);

    @Test
    void combinesRuleAndAiScoresToRerankCandidates() throws Exception {
        when(openAiClient.generateStructured(eq("route_ranking"), any(), any(), any()))
                .thenReturn("""
                        {"rankings":[
                          {"cargoId":1,"aiScore":0,"reasons":[],"concerns":[]},
                          {"cargoId":2,"aiScore":100,"reasons":["선호 화물"],"concerns":[]}
                        ]}
                        """);

        List<LoadResponse> result = reranker.rerank(1L, "운임 65만 원 이상",
                List.of(load(1L, 90, 600000), load(2L, 85, 700000)), false).loads();

        assertThat(result).extracting(LoadResponse::cargoId).containsExactly(2L, 1L);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> schemaCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).generateStructured(eq("route_ranking"),
                instructionsCaptor.capture(), inputCaptor.capture(), schemaCaptor.capture());

        assertThat(instructionsCaptor.getValue())
                .contains("기사 선호 적합도만")
                .contains("baseScore를 복사·재계산")
                .contains("baseScore 80%, aiScore 20%")
                .contains("누락 정보는 중립")
                .contains("50: 선호가 없거나 후보 정보로 관련성을 판단할 수 없다");
        JsonNode input = objectMapper.readTree(inputCaptor.getValue());
        assertThat(input.at("/driverPreference").asText()).isEqualTo("운임 65만 원 이상");
        assertThat(input.at("/candidates/0/origin").asText()).isEqualTo("서울 송파구");
        assertThat(input.at("/candidates/0/destination").asText()).isEqualTo("부산 강서구");
        assertThat(input.at("/candidates/0/weightTon").decimalValue()).isEqualByComparingTo("5.0");
        assertThat(input.at("/candidates/0/tripDistanceKm").asInt()).isEqualTo(325);
        assertThat(input.at("/candidates/0/vehicleType").asText()).isEqualTo("CARGO");
        assertThat(input.at("/candidates/0/bodyType").asText()).isEqualTo("WING");
        assertThat(schemaCaptor.getValue()
                .at("/properties/rankings/items/properties/reasons/items/maxLength").asInt())
                .isEqualTo(120);
    }

    @Test
    void fallsBackWhenAiReturnsUnknownCargoId() {
        when(openAiClient.generateStructured(eq("route_ranking"), any(), any(), any()))
                .thenReturn("""
                        {"rankings":[
                          {"cargoId":1,"aiScore":100,"reasons":[],"concerns":[]},
                          {"cargoId":999,"aiScore":100,"reasons":[],"concerns":[]}
                        ]}
                        """);

        List<LoadResponse> original = List.of(load(1L, 90, 600000), load(2L, 85, 700000));

        assertThat(reranker.rerank(1L, "냉장 화물 우선", original, false).loads()).isSameAs(original);
    }

    @Test
    void reusesSuccessfulAiRankingForSameDriverAndCandidates() {
        when(openAiClient.generateStructured(eq("route_ranking"), any(), any(), any()))
                .thenReturn("""
                        {"rankings":[
                          {"cargoId":1,"aiScore":90,"reasons":[],"concerns":[]},
                          {"cargoId":2,"aiScore":90,"reasons":[],"concerns":[]}
                        ]}
                        """);
        List<LoadResponse> loads = List.of(load(1L, 90, 600000), load(2L, 85, 700000));

        reranker.rerank(1L, "냉장 화물 우선", loads, false);
        reranker.rerank(1L, "냉장 화물 우선", loads, false);

        Mockito.verify(openAiClient, Mockito.times(1)).generateStructured(eq("route_ranking"), any(), any(), any());
    }

    private LoadResponse load(Long cargoId, int score, int fare) {
        return new LoadResponse(cargoId, "서울 송파구", "부산 강서구",
                LocalDateTime.of(2026, 8, 14, 8, 0), LocalDateTime.of(2026, 8, 14, 15, 0),
                325, "CARGO", new BigDecimal("5.0"), "WING", "REFRIGERATED", fare, score,
                null, null, null, null, null, null, null, "RULE_BASE", List.of(),
                "REQUESTED", null);
    }
}
