package com.hackathon.domain.cargo;

import static org.hamcrest.Matchers.containsString;
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

/** 화주 페이지의 AI 연동 지점(자연어 파싱, 시세 추정)이 TODO 상태를 정직하게 알려주는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class AiTodoEndpointsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("자연어 파싱은 AI 미연동 상태라 501과 TODO 메시지를 반환한다")
    void parseIsTodo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\": \"내일 서울→부산 냉장 5톤 예산 50만원\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("TODO(AI)")));
    }

    @Test
    @DisplayName("시세 조회는 캐시 미스면 AI 미연동 501을 반환한다")
    void quoteIsTodoOnCacheMiss() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/fares/quote")
                        .param("originSido", "강원특별자치도")
                        .param("destSido", "강원특별자치도")
                        .param("cargoType", "GENERAL")
                        .param("weightTon", "1.0"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.message").value(containsString("TODO(AI)")));
    }
}
