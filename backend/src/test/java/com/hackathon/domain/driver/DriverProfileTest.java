package com.hackathon.domain.driver;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class DriverProfileTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("기사 프로필 전체 정보를 조회한다")
    void returnsFullDriverProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.vehicleCargoTypes[0]").value("GENERAL"))
                .andExpect(jsonPath("$.data.contactableFrom").value("06:00"))
                .andExpect(jsonPath("$.data.contactableTo").value("20:00"))
                .andExpect(jsonPath("$.data.recentTripSummary").value("수원→부산 · 3일 전"));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("기사 프로필을 수정하고 서버 관리 정보와 활동 지역은 유지한다")
    void updatesDriverProfile() throws Exception {
        String body = """
                {
                  "name": "홍길동",
                  "phoneNumber": "010-9876-5432",
                  "plateNumber": "99바 9999",
                  "vehicleType": "탑차",
                  "capacityTon": 11.0,
                  "bodyType": "냉동탑차",
                  "vehicleCargoTypes": ["REFRIGERATED", "FROZEN"],
                  "minAcceptFare": 300000,
                  "contactableFrom": "07:30",
                  "contactableTo": "19:00"
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.capacityTon").value(11.0))
                .andExpect(jsonPath("$.data.vehicleCargoTypes.length()").value(2))
                .andExpect(jsonPath("$.data.minAcceptFare").value(300000))
                .andExpect(jsonPath("$.data.rating").value(4.6))
                .andExpect(jsonPath("$.data.routePreferences.origins.length()").value(3));
    }

    @Test
    @DisplayName("기사 프로필 수정 요청 형식이 잘못되면 400을 반환한다")
    void rejectsInvalidProfile() throws Exception {
        String body = """
                {
                  "name": "",
                  "phoneNumber": "010-9876-5432",
                  "plateNumber": "99바 9999",
                  "vehicleType": "탑차",
                  "capacityTon": 0,
                  "bodyType": "냉동탑차",
                  "vehicleCargoTypes": ["REFRIGERATED"],
                  "minAcceptFare": 300000,
                  "contactableFrom": "25:00",
                  "contactableTo": "19:00"
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
