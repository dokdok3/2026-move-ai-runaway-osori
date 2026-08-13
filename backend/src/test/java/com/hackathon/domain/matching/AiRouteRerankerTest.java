package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.service.AiRouteReranker;
import com.hackathon.global.client.OpenAiClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiRouteRerankerTest {

    private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
    private final AiRouteReranker reranker = new AiRouteReranker(openAiClient, new ObjectMapper());

    @Test
    void combinesRuleAndAiScoresToRerankCandidates() {
        when(openAiClient.generateStructured(eq("route_ranking"), any(), any(), any()))
                .thenReturn("""
                        {"rankings":[
                          {"cargoId":1,"aiScore":0,"reasons":[],"concerns":[]},
                          {"cargoId":2,"aiScore":100,"reasons":["선호 화물"],"concerns":[]}
                        ]}
                        """);

        List<LoadResponse> result = reranker.rerank("냉장 화물 우선", List.of(load(1L, 90, 600000), load(2L, 85, 700000))).loads();

        assertThat(result).extracting(LoadResponse::cargoId).containsExactly(2L, 1L);
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

        assertThat(reranker.rerank("냉장 화물 우선", original).loads()).isSameAs(original);
    }

    private LoadResponse load(Long cargoId, int score, int fare) {
        return new LoadResponse(cargoId, "서울 송파구", "부산 강서구",
                LocalDateTime.of(2026, 8, 14, 8, 0), LocalDateTime.of(2026, 8, 14, 15, 0),
                null, null, new BigDecimal("5.0"), null, "REFRIGERATED", fare, score,
                null, null, null, null, null, null, null, "RULE_BASE", List.of());
    }
}
