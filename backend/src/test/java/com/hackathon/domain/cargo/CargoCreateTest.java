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
                .andExpect(jsonPath("$.data.fare.averageFare").isNumber())
                .andExpect(jsonPath("$.data.assignedDriver").doesNotExist());
    }

    @Test
    @DisplayName("화주는 본인이 등록한 화물만 최신순으로 조회한다")
    void returnsOnlyMyCargos() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cargos/me")
                        .header("X-User-Id", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.data[0].origin.sido").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].unloadingAt").value("2026-08-12T18:00:00"));
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("희망 운임을 입력해주세요."));
    }

    @Test
    @DisplayName("하차 일시가 없으면 400")
    void rejectsMissingUnloadingAt() throws Exception {
        String invalid = BODY.replace("\"unloadingAt\": \"2026-08-12T18:00:00\",", "\"unloadingAt\": null,");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("하차 일시를 입력해주세요."));
    }

    @Test
    @DisplayName("출발 시군구가 없으면 400")
    void rejectsMissingOriginSigungu() throws Exception {
        String invalid = BODY.replace("\"sigungu\": \"강남구\"", "\"sigungu\": null");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("시군구를 입력해주세요."));
    }
}
