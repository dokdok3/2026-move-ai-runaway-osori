package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.service.FareQuoteService;
import com.hackathon.domain.matching.service.LoadService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoadFilterFareInfoTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LoadService loadService;

    @Autowired
    CargoRepository cargoRepository;

    @MockitoBean
    FareQuoteService fareQuoteService;

    @Test
    @DisplayName("수락 및 숨김 목록에도 시세 정보가 포함된다")
    void returnsFareInfoForAcceptedAndHiddenLoads() throws Exception {
        when(fareQuoteService.quote(any(), any(), any(), any(), any()))
                .thenReturn(new FareQuoteResponse(1_000_000, 800_000, 380, "SLOW", null));
        Long acceptedCargoId = createCargo(600_000);
        Long hiddenCargoId = createCargo(900_000);
        loadService.accept(1L, acceptedCargoId);
        loadService.hide(1L, hiddenCargoId);

        JsonNode accepted = findLoad("ACCEPTED", acceptedCargoId);
        JsonNode hidden = findLoad("HIDDEN", hiddenCargoId);

        assertThat(accepted.get("badge").asText()).isEqualTo("LOW");
        assertThat(accepted.get("regionAverageFare").asInt()).isEqualTo(1_000_000);
        assertThat(accepted.get("belowPercent").asInt()).isEqualTo(40);
        assertThat(hidden.get("badge").asText()).isEqualTo("FAIR");
        assertThat(hidden.get("regionAverageFare").asInt()).isEqualTo(1_000_000);
        assertThat(hidden.get("belowPercent").isNull()).isTrue();
    }

    private JsonNode findLoad(String filter, Long cargoId) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1")
                        .param("filter", filter))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data")
                .path("content");
        return StreamSupport.stream(content.spliterator(), false)
                .filter(node -> node.get("cargoId").asLong() == cargoId)
                .findFirst()
                .orElseThrow();
    }

    private Long createCargo(int fare) {
        LocalDateTime loadingAt = LocalDateTime.now().plusDays(2);
        Cargo cargo = Cargo.create(
                300L,
                "경기도", "수원시",
                "부산광역시", "강서구",
                CargoType.GENERAL, "필터 시세 테스트 화물", new BigDecimal("3.0"),
                "카고", "윙바디", fare,
                loadingAt, loadingAt.plusHours(8), 380
        );
        return cargoRepository.save(cargo).getId();
    }
}
