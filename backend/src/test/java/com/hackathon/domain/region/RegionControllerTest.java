package com.hackathon.domain.region;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "CREATE TABLE region_coordinate (sido VARCHAR(30) NOT NULL, sigungu VARCHAR(30) NOT NULL, "
                + "PRIMARY KEY (sido, sigungu))",
        "INSERT INTO region_coordinate (sido, sigungu) VALUES "
                + "('서울특별시', '강남구'), ('인천광역시', '부평구'), ('인천광역시', '서구')"
})
@Sql(statements = "DROP TABLE region_coordinate", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
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
                .andExpect(jsonPath("$.data[0].sigungus[0]").value("강남구"))
                .andExpect(jsonPath("$.data[1].sido").value("인천광역시"))
                .andExpect(jsonPath("$.data[1].sigungus", hasItem("부평구")));
    }
}
