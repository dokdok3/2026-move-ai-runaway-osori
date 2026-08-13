package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.domain.matching.service.LoadService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class LoadCompletionTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LoadService loadService;

    @Autowired
    CargoRepository cargoRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Test
    @DisplayName("수락한 기사가 화물을 완료하면 완료 상태와 완료 시각을 반환한다")
    void completesAcceptedLoad() throws Exception {
        Long cargoId = createCargo();
        loadService.accept(1L, cargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/complete", cargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cargoId").value(cargoId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.COMPLETED);
        assertThat(assignmentRepository.findByCargoId(cargoId).orElseThrow().getCompletedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("완료 목록에는 해당 기사가 완료한 화물만 반환한다")
    void returnsOnlyCompletedLoads() throws Exception {
        Long completedCargoId = createCargo();
        Long matchedCargoId = createCargo();
        Long otherDriverCargoId = createCargo();
        loadService.accept(1L, completedCargoId);
        loadService.accept(1L, matchedCargoId);
        loadService.accept(2L, otherDriverCargoId);
        loadService.complete(1L, completedCargoId);
        loadService.complete(2L, otherDriverCargoId);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads/completed")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
        List<Long> cargoIds = StreamSupport.stream(data.spliterator(), false)
                .map(node -> node.get("cargoId").asLong())
                .toList();

        assertThat(cargoIds)
                .contains(completedCargoId)
                .doesNotContain(matchedCargoId, otherDriverCargoId);
        assertThat(data).allSatisfy(node ->
                assertThat(node.get("status").asText()).isEqualTo("COMPLETED"));
    }

    @Test
    @DisplayName("수락하지 않은 기사는 다른 기사의 화물을 완료할 수 없다")
    void rejectsOtherDriver() throws Exception {
        Long cargoId = createCargo();
        loadService.accept(1L, cargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/complete", cargoId)
                        .header("X-User-Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인이 수락한 화물만 완료할 수 있습니다."));

        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.MATCHED);
        assertThat(assignmentRepository.findByCargoId(cargoId).orElseThrow().getCompletedAt())
                .isNull();
    }

    @Test
    @DisplayName("수락되지 않은 화물은 완료할 수 없다")
    void rejectsUnassignedCargo() throws Exception {
        Long cargoId = createCargo();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/complete", cargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("수락되지 않은 화물은 완료할 수 없습니다."));

        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.REQUESTED);
    }

    @Test
    @DisplayName("이미 완료된 화물을 다시 완료할 수 없다")
    void rejectsAlreadyCompletedCargo() throws Exception {
        Long cargoId = createCargo();
        loadService.accept(1L, cargoId);
        loadService.complete(1L, cargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/complete", cargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 완료된 화물입니다."));
    }

    private Long createCargo() {
        LocalDateTime loadingAt = LocalDateTime.now().plusDays(2);
        Cargo cargo = Cargo.create(
                300L,
                "경기도", "수원시",
                "부산광역시", "강서구",
                CargoType.GENERAL, "완료 테스트 화물", new BigDecimal("3.0"),
                "카고", "윙바디", 800_000,
                loadingAt, loadingAt.plusHours(8), 380
        );
        return cargoRepository.save(cargo).getId();
    }
}
