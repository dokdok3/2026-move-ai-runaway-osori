package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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
    @DisplayName("배송 완료 시 이어서 운송할 최적 화물 1건과 추천 이유를 반환한다")
    void returnsOneConnectionRecommendationOnCompletion() throws Exception {
        LocalDateTime completedAround = LocalDateTime.now();
        LocalDateTime loadingAt = completedAround.minusHours(9);
        LocalDateTime unloadingAt = completedAround.minusHours(1);
        Long completedCargoId = saveCargo(
                "경기도", "수원시", "제주특별자치도", "제주시",
                800_000, loadingAt, unloadingAt
        );
        Long bestNextCargoId = saveCargo(
                "제주특별자치도", "제주시", "서울특별시", "강남구",
                900_000, completedAround.plusHours(1), completedAround.plusHours(8)
        );
        saveCargo(
                "제주특별자치도", "제주시", "부산광역시", "강서구",
                700_000, completedAround.plusHours(2), completedAround.plusHours(10)
        );
        loadService.accept(1L, completedCargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/complete", completedCargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextLoadRecommendation").isMap())
                .andExpect(jsonPath("$.data.nextLoadRecommendation.cargoId").value(bestNextCargoId))
                .andExpect(jsonPath("$.data.nextLoadRecommendation.emptyDistanceKm").value(0.0))
                .andExpect(jsonPath("$.data.nextLoadRecommendation.waitMinutes")
                        .value(allOf(greaterThanOrEqualTo(59), lessThanOrEqualTo(60))))
                .andExpect(jsonPath("$.data.nextLoadRecommendation.recommendationSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.nextLoadRecommendation.recommendationReasons.length()").value(3))
                .andExpect(jsonPath("$.data.nextLoadRecommendation.explanationMode")
                        .value("RULE_FALLBACK"));
    }

    @Test
    @DisplayName("48시간 안에 이어갈 화물이 없으면 완료 응답의 추천은 null이다")
    void returnsNullWhenNoConnectionCandidateExists() throws Exception {
        LocalDateTime completedAround = LocalDateTime.now();
        Long completedCargoId = saveCargo(
                "경기도", "수원시", "세종특별자치시", "세종시",
                800_000, completedAround.minusHours(9), completedAround.minusHours(1)
        );
        saveCargo(
                "세종특별자치시", "세종시", "서울특별시", "강남구",
                900_000, completedAround.plusHours(49), completedAround.plusHours(57)
        );
        loadService.accept(1L, completedCargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/complete", completedCargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.nextLoadRecommendation").value(nullValue()));
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
        return saveCargo(
                "경기도", "수원시", "부산광역시", "강서구",
                800_000, loadingAt, loadingAt.plusHours(8));
    }

    private Long saveCargo(
            String originSido,
            String originSigungu,
            String destSido,
            String destSigungu,
            int fare,
            LocalDateTime loadingAt,
            LocalDateTime unloadingAt
    ) {
        Cargo cargo = Cargo.create(
                300L,
                originSido, originSigungu,
                destSido, destSigungu,
                CargoType.GENERAL, "완료 테스트 화물", new BigDecimal("3.0"),
                "카고", "윙바디", fare,
                loadingAt, unloadingAt, 380
        );
        return cargoRepository.save(cargo).getId();
    }
}
