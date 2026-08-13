package com.hackathon.domain.cargo;

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
    @DisplayName("파싱값이 부족해도 성공 응답에 실제 누락 항목을 한글로 반환한다")
    void returnsActualMissingFieldsInKorean() throws Exception {
        when(openAiClient.generateStructured(any(), any(), any(JsonNode.class)))
                .thenReturn("""
                        {
                          "origin":{"sido":"서울특별시","sigungu":"전체","detail":null},
                          "destination":{"sido":null,"sigungu":"화순군","detail":null},
                          "cargoType":null,
                          "cargoDescription":"박스 1개",
                          "weightTon":1,
                          "offeredFareKrw":50000000,
                          "loadingDate":"2026-08-14",
                          "loadingTimeText":null,
                          "unloadingDate":null,
                          "unloadingTimeText":null,
                          "missingFields":[],
                          "confidence":"LOW",
                          "warnings":[]
                        }
                        """);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestText":"내일 서울에서 화순으로 박스 1개 1톤 50000000원",
                                  "referenceDate":"2026-08-13"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.origin.sido").value("서울특별시"))
                .andExpect(jsonPath("$.data.destination.sigungu").value("화순군"))
                .andExpect(jsonPath("$.data.missingFields[0]").value("출발지 시군구는 필수입니다."))
                .andExpect(jsonPath("$.data.missingFields[1]").value("도착지 시도는 필수입니다."))
                .andExpect(jsonPath("$.data.missingFields[2]").value("화물 종류는 필수입니다."))
                .andExpect(jsonPath("$.data.missingFields[3]").value("하차일은 필수입니다."));
    }

    @Test
    @DisplayName("시세 조회는 캐시 미스여도 기본 시세를 생성해 반환한다")
    void quoteReturnsFallbackOnCacheMiss() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/fares/quote")
                        .param("originSido", "강원특별자치도")
                        .param("destSido", "강원특별자치도")
                        .param("cargoType", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageFare").isNumber())
                .andExpect(jsonPath("$.data.sameDayThreshold").isNumber());
    }
}
