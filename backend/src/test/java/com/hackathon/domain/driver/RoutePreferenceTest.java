package com.hackathon.domain.driver;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class RoutePreferenceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("다니는 구간을 전체 교체하고 저장 결과를 반환한다")
    void replacesPreferences() throws Exception {
        String body = """
                {
                  "origins": [
                    {"sido": "충청남도", "sigungu": "천안시"}
                  ],
                  "destinations": [
                    {"sido": "경기도", "sigungu": null}
                  ]
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routePreferences.origins.length()").value(1))
                .andExpect(jsonPath("$.data.routePreferences.origins[0].sido").value("충청남도"))
                .andExpect(jsonPath("$.data.routePreferences.origins[0].sigungu").value("천안시"))
                .andExpect(jsonPath("$.data.routePreferences.destinations.length()").value(1))
                .andExpect(jsonPath("$.data.routePreferences.destinations[0].sido").value("경기도"))
                .andExpect(jsonPath("$.data.routePreferences.destinations[0].sigungu").doesNotExist());

        // 다시 조회해도 교체된 값이 유지된다
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.routePreferences.origins.length()").value(1));
    }

    @Test
    @DisplayName("origins가 비어 있으면 400")
    void rejectsEmptyOrigins() throws Exception {
        String body = """
                {"origins": [], "destinations": [{"sido": "경기도", "sigungu": null}]}
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
