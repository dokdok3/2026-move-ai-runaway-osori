package com.hackathon.domain.cargo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.hackathon.domain.region.repository.RegionCoordinateRepository;
import com.hackathon.domain.region.repository.RegionCoordinateRepository.RegionCoordinate;
import com.hackathon.global.client.OpenAiClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    @MockitoBean
    RegionCoordinateRepository regionCoordinateRepository;

    @BeforeEach
    void resetMocks() {
        reset(openAiClient, regionCoordinateRepository);
        when(regionCoordinateRepository.findBySigungu("송파구"))
                .thenReturn(List.of(new RegionCoordinate("서울특별시", "송파구")));
        when(regionCoordinateRepository.findBySigungu("강서구")).thenReturn(List.of(
                new RegionCoordinate("서울특별시", "강서구"),
                new RegionCoordinate("부산광역시", "강서구")
        ));
        when(regionCoordinateRepository.findBySigungu("청주시"))
                .thenReturn(List.of(new RegionCoordinate("충청북도", "청주시")));
        when(regionCoordinateRepository.findBySigungu("화순군"))
                .thenReturn(List.of(new RegionCoordinate("전라남도", "화순군")));
        when(regionCoordinateRepository.findBySigungu("서구")).thenReturn(List.of(
                new RegionCoordinate("대전광역시", "서구"),
                new RegionCoordinate("인천광역시", "서구")
        ));
    }

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

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> schemaCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).generateStructured(
                instructionsCaptor.capture(), inputCaptor.capture(), schemaCaptor.capture());

        assertThat(instructionsCaptor.getValue())
                .contains("\"청쥬시\"는 \"청주시\"로 교정")
                .contains("지역명 오타를 교정한 경우 warnings")
                .contains("같은 시군구")
                .contains("명백한 오타와 띄어쓰기만")
                .contains("상품명·상태·수량 같은 구체 정보를 유지")
                .contains("\"내동 화물\"은 cargoType=FROZEN, cargoDescription=\"냉동 화물\"")
                .contains("오타를 교정한 경우 warnings");
        assertThat(inputCaptor.getValue())
                .contains("CargoType enum")
                .contains("REFRIGERATED: 냉장 화물")
                .contains("GENERAL: 일반 화물")
                .contains("FROZEN: 냉동 화물")
                .contains("CONSTRUCTION: 건설 화물")
                .contains("HAZARDOUS: 위험물")
                .doesNotContain("region_coordinate", "충청북도: 청주시");
        assertThat(schemaCaptor.getValue().at("/properties/cargoType/enum").toString())
                .contains("REFRIGERATED", "GENERAL", "FROZEN", "CONSTRUCTION", "HAZARDOUS")
                .doesNotContain("OTHER");
    }

    @Test
    @DisplayName("AI가 추출한 시군구를 DB와 대조해 시도를 보완한다")
    void enrichesSidoFromRegionCoordinateAfterAiParsing() throws Exception {
        when(openAiClient.generateStructured(any(), any(), any(JsonNode.class)))
                .thenReturn("""
                        {
                          "origin":{"sido":null,"sigungu":"청주시","detail":null},
                          "destination":{"sido":"부산광역시","sigungu":"강서구","detail":null},
                          "cargoType":"GENERAL",
                          "cargoDescription":"일반 화물",
                          "weightTon":1,
                          "offeredFareKrw":300000,
                          "loadingDate":"2026-08-14",
                          "loadingTimeText":null,
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
                                  "requestText":"내일 청주시에서 부산 강서구로 일반 화물 1톤 30만원",
                                  "referenceDate":"2026-08-13"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.origin.sido").value("충청북도"))
                .andExpect(jsonPath("$.data.origin.sigungu").value("청주시"))
                .andExpect(jsonPath("$.data.missingFields").isEmpty());

        InOrder callOrder = inOrder(openAiClient, regionCoordinateRepository);
        callOrder.verify(openAiClient).generateStructured(any(), any(), any(JsonNode.class));
        callOrder.verify(regionCoordinateRepository).findBySigungu("청주시");
    }

    @Test
    @DisplayName("AI가 시군구 오타를 정식 지역명으로 교정한 결과를 DB와 대조한다")
    void validatesAiCorrectedSigunguAgainstRegionCoordinate() throws Exception {
        when(openAiClient.generateStructured(any(), any(), any(JsonNode.class)))
                .thenReturn("""
                        {
                          "origin":{"sido":"충청북도","sigungu":"청주시","detail":null},
                          "destination":{"sido":"부산광역시","sigungu":"강서구","detail":null},
                          "cargoType":"GENERAL",
                          "cargoDescription":"일반 화물",
                          "weightTon":1,
                          "offeredFareKrw":300000,
                          "loadingDate":"2026-08-14",
                          "loadingTimeText":null,
                          "unloadingDate":"2026-08-14",
                          "unloadingTimeText":null,
                          "missingFields":[],
                          "confidence":"HIGH",
                          "warnings":["'청쥬시'를 '청주시'로 보정했습니다."]
                        }
                        """);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestText\":\"청쥬시에서 부산 강서구로 화물\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.origin.sido").value("충청북도"))
                .andExpect(jsonPath("$.data.origin.sigungu").value("청주시"))
                .andExpect(jsonPath("$.data.warnings[0]").value("'청쥬시'를 '청주시'로 보정했습니다."));
    }

    @Test
    @DisplayName("여러 시도에 있는 시군구는 시도가 없으면 추측하지 않는다")
    void doesNotGuessAmbiguousSigungu() throws Exception {
        when(openAiClient.generateStructured(any(), any(), any(JsonNode.class)))
                .thenReturn("""
                        {
                          "origin":{"sido":null,"sigungu":"서구","detail":null},
                          "destination":{"sido":"부산광역시","sigungu":"강서구","detail":null},
                          "cargoType":"GENERAL",
                          "cargoDescription":"일반 화물",
                          "weightTon":1,
                          "offeredFareKrw":300000,
                          "loadingDate":"2026-08-14",
                          "loadingTimeText":null,
                          "unloadingDate":"2026-08-14",
                          "unloadingTimeText":null,
                          "missingFields":[],
                          "confidence":"LOW",
                          "warnings":[]
                        }
                        """);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestText\":\"서구에서 부산 강서구로 화물\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.origin.sido").doesNotExist())
                .andExpect(jsonPath("$.data.origin.sigungu").value("서구"))
                .andExpect(jsonPath("$.data.missingFields[0]").value("출발지 시도는 필수입니다."));
    }

    @Test
    @DisplayName("OpenAI 호출 실패는 원문을 노출하지 않고 502를 반환한다")
    void parseFailureReturnsBadGateway() throws Exception {
        when(openAiClient.generateStructured(any(), any(), any(JsonNode.class)))
                .thenThrow(new IllegalStateException("upstream detail"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestText\":\"노출되면 안 되는 화물 원문\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("AI 호출에 실패했습니다."));

        verifyNoInteractions(regionCoordinateRepository);
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
                .andExpect(jsonPath("$.data.destination.sido").value("전라남도"))
                .andExpect(jsonPath("$.data.destination.sigungu").value("화순군"))
                .andExpect(jsonPath("$.data.missingFields[0]").value("출발지 시군구는 필수입니다."))
                .andExpect(jsonPath("$.data.missingFields[1]").value("화물 종류는 필수입니다."))
                .andExpect(jsonPath("$.data.missingFields[2]").value("하차일은 필수입니다."));
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
