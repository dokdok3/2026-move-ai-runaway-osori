package com.hackathon.domain.region;

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
class RegionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("시도별 시군구 트리를 반환한다")
    void returnsRegionTree() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sido").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].sigungus[0]").value("강남구"));
    }
}
