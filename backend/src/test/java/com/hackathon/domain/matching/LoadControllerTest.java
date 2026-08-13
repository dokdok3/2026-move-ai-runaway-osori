package com.hackathon.domain.matching;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class LoadControllerTest {

    @Autowired
    MockMvc mockMvc;

    /** 다른 테스트가 구간을 바꿔놨을 수 있으므로 매번 seed 구간으로 되돌린다. */
    @BeforeEach
    void resetPreferences() throws Exception {
        String body = """
                {
                  "origins": [
                    {"sido": "경기도", "sigungu": "수원시"},
                    {"sido": "경기도", "sigungu": "용인시"},
                    {"sido": "서울특별시", "sigungu": null}
                  ],
                  "destinations": [
                    {"sido": "부산광역시", "sigungu": null},
                    {"sido": "서울특별시", "sigungu": "송파구"}
                  ]
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("구간에 맞는 화물만 점수순으로 반환하고 시세 정보를 함께 제공한다")
    void returnsScoredLoads() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].badge").value("FAIR"))
                .andExpect(jsonPath("$.data.content[0].regionAverageFare").isNumber())
                .andExpect(jsonPath("$.data.content[0].origin").value("경기 수원시"))
                .andExpect(jsonPath("$.data.content[0].destination").value("부산 강서구"))
                .andExpect(jsonPath("$.data.content[0].matchScore").value(100))
                .andExpect(jsonPath("$.data.size").value(10))
                // 광주→전주(구간 밖)는 목록에 없어야 한다
                .andExpect(jsonPath("$.data.content[?(@.origin == '광주 광산구')]").isEmpty());
    }

    @Test
    @DisplayName("프론트 필터 ALL로 매칭 가능한 화물을 조회한다")
    void returnsAvailableLoadsWithAllFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1")
                        .param("filter", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("시군구 전체를 선택하면 같은 시도의 모든 시군구 화물이 조회된다")
    void returnsLoadsForWholeSidoPreferences() throws Exception {
        String body = """
                {
                  "origins": [{"sido": "경기도", "sigungu": null}],
                  "destinations": [{"sido": "부산광역시", "sigungu": null}]
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].origin").value("경기 수원시"))
                .andExpect(jsonPath("$.data.content[0].destination").value("부산 강서구"));
    }

    @Test
    @DisplayName("프론트 필터 HIDDEN으로 숨긴 화물 목록을 조회한다")
    void returnsHiddenLoadsWithHiddenFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1")
                        .param("filter", "HIDDEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("프론트 필터 ACCEPTED로 수락한 화물 목록을 조회한다")
    void returnsAcceptedLoadsWithAcceptedFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1")
                        .param("filter", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("시세 조회에 성공한 모든 화물에 평균 운임과 판정을 제공한다")
    void returnsFareInfoForEveryLoad() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].badge", everyItem(anyOf(is("LOW"), is("FAIR")))))
                .andExpect(jsonPath("$.data.content[*].regionAverageFare", everyItem(notNullValue())));
    }

    @Test
    @DisplayName("점수는 내림차순으로 정렬된다")
    void sortedByScoreDesc() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    java.util.regex.Matcher m =
                            java.util.regex.Pattern.compile("\"matchScore\":(\\d+)").matcher(json);
                    int previous = Integer.MAX_VALUE;
                    while (m.find()) {
                        int score = Integer.parseInt(m.group(1));
                        if (score > previous) {
                            throw new AssertionError("정렬이 깨졌습니다: " + json);
                        }
                        previous = score;
                    }
                });
    }

    @Test
    @DisplayName("구간을 바꾸면 결과가 비어 목록이 없다")
    void emptyWhenNoMatch() throws Exception {
        String body = """
                {
                  "origins": [{"sido": "테스트특별시", "sigungu": null}],
                  "destinations": [{"sido": "테스트특별시", "sigungu": null}]
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
}
