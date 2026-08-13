package com.hackathon.domain.cargo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CargoCreateTest {

    @Autowired
    MockMvc mockMvc;

    private static final String BODY = """
            {
              "origin": {"sido": "서울특별시", "sigungu": "강남구"},
              "destination": {"sido": "부산광역시", "sigungu": "해운대구"},
              "cargoType": "REFRIGERATED",
              "cargoDescription": "냉장식품",
              "weightTon": 5.0,
              "vehicleType": "탑차",
              "bodyType": null,
              "desiredFare": 500000,
              "loadingAt": "2026-08-12T14:00:00",
              "unloadingAt": "2026-08-12T18:00:00",
              "distanceKm": 325
            }
            """;

    @Test
    @DisplayName("화물을 등록하면 REQUESTED 상태로 저장되고 상세 조회가 가능하다")
    void createsAndReadsCargo() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.cargoId").isNumber())
                .andReturn().getResponse().getContentAsString();

        String cargoId = response.replaceAll(".*\"cargoId\":(\\d+).*", "$1");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cargos/" + cargoId)
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.origin.sido").value("서울특별시"))
                .andExpect(jsonPath("$.data.destination.sigungu").value("해운대구"))
                .andExpect(jsonPath("$.data.cargoType").value("REFRIGERATED"))
                .andExpect(jsonPath("$.data.cargoDescription").value("냉장식품"))
                .andExpect(jsonPath("$.data.desiredFare").value(500000))
                // 시세 캐시가 비어 있으므로 AI 미연동이어도 화물 조회는 막히지 않고 fare만 비어야 한다.
                .andExpect(jsonPath("$.data.fare").doesNotExist())
                .andExpect(jsonPath("$.data.assignedDriver").doesNotExist());
    }

    @Test
    @DisplayName("없는 화물을 조회하면 404")
    void rejectsUnknownCargo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cargos/999999")
                        .header("X-User-Id", "100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("희망 운임이 없으면 400")
    void rejectsMissingFare() throws Exception {
        String invalid = BODY.replace("\"desiredFare\": 500000,", "\"desiredFare\": null,");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }
}
