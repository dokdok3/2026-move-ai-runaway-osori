package com.hackathon.domain.driver;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class DriverControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("seed 기사 프로필과 다니는 구간을 반환한다")
    void returnsDriverProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driverId").value(1))
                .andExpect(jsonPath("$.data.vehicleType").value("카고"))
                .andExpect(jsonPath("$.data.capacityTon").value(25.0))
                .andExpect(jsonPath("$.data.minAcceptFare").value(150000))
                .andExpect(jsonPath("$.data.routePreferences.origins").isArray())
                .andExpect(jsonPath("$.data.routePreferences.destinations").isArray());
    }

    @Test
    @DisplayName("X-User-Id 헤더가 없으면 401")
    void rejectsMissingHeader() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 기사면 404")
    void rejectsUnknownDriver() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "9999"))
                .andExpect(status().isNotFound());
    }
}
