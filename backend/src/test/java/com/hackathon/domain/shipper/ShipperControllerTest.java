package com.hackathon.domain.shipper;

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
class ShipperControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("기존 화주 ID의 빈 프로필을 조회한다")
    void returnsExistingShipperProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/shippers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipperId").value(1));
    }

    @Test
    @DisplayName("화주 프로필을 수정한다")
    void updatesShipperProfile() throws Exception {
        String request = """
                {"companyName":"오소리 물류","contactName":"김화주","phoneNumber":"010-1234-5678",
                 "businessNumber":"123-45-67890","address":"서울특별시 강남구 테헤란로 123"}
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/shippers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("오소리 물류"))
                .andExpect(jsonPath("$.data.contactName").value("김화주"));
    }
}
