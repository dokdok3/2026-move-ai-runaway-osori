package com.hackathon.domain.cargo;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.hackathon.global.client.OpenAiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 화주 페이지의 AI 연동 지점을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class AiTodoEndpointsTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OpenAiClient openAiClient;

    @Test
    @DisplayName("자연어 파싱 결과를 API 응답으로 반환한다")
    void parsesFreightRequest() throws Exception {
        when(openAiClient.generateStructured(any(), contains("referenceDate: 2026-08-13"), any(JsonNode.class)))
                .thenReturn("""
                        {
                          "origin":{"sido":"서울특별시","sigungu":"송파구","detail":null},
                          "destination":{"sido":"부산광역시","sigungu":"강서구","detail":null},
                          "cargoType":"REFRIGERATED",
                          "cargoDescription":"냉장식품",
                          "weightTon":5,
                          "offeredFareKrw":500000,
                          "loadingDate":"2026-08-14",
                          "loadingTimeText":"오전",
                          "unloadingDate":"2026-08-14",
                          "unloadingTimeText":null,
                          "missingFields":[],
                          "confidence":"HIGH",
                          "warnings":[]
                        }
                        """);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestText":"내일 서울 송파에서 부산 강서로 냉장식품 5톤, 예산 50만원",
                                  "referenceDate":"2026-08-13"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.origin.sido").value("서울특별시"))
                .andExpect(jsonPath("$.data.destination.sigungu").value("강서구"))
                .andExpect(jsonPath("$.data.cargoType").value("REFRIGERATED"))
                .andExpect(jsonPath("$.data.offeredFareKrw").value(500000))
                .andExpect(jsonPath("$.data.loadingDate").value("2026-08-14"))
                .andExpect(jsonPath("$.data.confidence").value("HIGH"));
    }

    @Test
    @DisplayName("OpenAI 호출 실패는 원문을 노출하지 않고 502를 반환한다")
    void parseFailureReturnsBadGateway() throws Exception {
        reset(openAiClient);
        when(openAiClient.generateStructured(any(), any(), any(JsonNode.class)))
                .thenThrow(new IllegalStateException("upstream detail"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestText\":\"노출되면 안 되는 화물 원문\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("AI 호출에 실패했습니다."));
    }

    @Test
    @DisplayName("시세 조회는 캐시 미스면 AI 미연동 501을 반환한다")
    void quoteIsTodoOnCacheMiss() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/fares/quote")
                        .param("originSido", "강원특별자치도")
                        .param("destSido", "강원특별자치도")
                        .param("cargoType", "GENERAL")
                        .param("weightTon", "1.0"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.message").value(containsString("TODO(AI)")));
    }
}
