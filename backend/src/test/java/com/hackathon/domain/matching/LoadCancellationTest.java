package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.domain.matching.service.LoadService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoadCancellationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LoadService loadService;

    @Autowired
    CargoRepository cargoRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Test
    @DisplayName("수락한 기사가 화물을 취소하면 다시 요청 상태가 된다")
    void cancelsAcceptedLoad() throws Exception {
        Long cargoId = createCargo();
        loadService.accept(1L, cargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/cancel", cargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cargoId").value(cargoId))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));

        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.REQUESTED);
        assertThat(assignmentRepository.findByCargoId(cargoId)).isEmpty();

        loadService.accept(2L, cargoId);
        assertThat(assignmentRepository.findByCargoId(cargoId).orElseThrow().getDriverId())
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("다른 기사가 수락한 화물은 취소할 수 없다")
    void rejectsOtherDriver() throws Exception {
        Long cargoId = createCargo();
        loadService.accept(1L, cargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/cancel", cargoId)
                        .header("X-User-Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인이 수락한 화물만 취소할 수 있습니다."));

        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.MATCHED);
        assertThat(assignmentRepository.findByCargoId(cargoId)).isPresent();
    }

    @Test
    @DisplayName("수락되지 않은 화물은 취소할 수 없다")
    void rejectsUnacceptedLoad() throws Exception {
        Long cargoId = createCargo();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/cancel", cargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("수락되지 않은 화물은 취소할 수 없습니다."));
    }

    @Test
    @DisplayName("완료한 화물은 수락 취소할 수 없다")
    void rejectsCompletedLoad() throws Exception {
        Long cargoId = createCargo();
        loadService.accept(1L, cargoId);
        loadService.complete(1L, cargoId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/loads/{cargoId}/cancel", cargoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("완료된 화물은 수락 취소할 수 없습니다."));

        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.COMPLETED);
        assertThat(assignmentRepository.findByCargoId(cargoId)).isPresent();
    }

    private Long createCargo() {
        LocalDateTime loadingAt = LocalDateTime.now().plusDays(2);
        Cargo cargo = Cargo.create(
                300L,
                "경기도", "수원시",
                "부산광역시", "강서구",
                CargoType.GENERAL, "수락 취소 테스트 화물", new BigDecimal("3.0"),
                "카고", "윙바디", 800_000,
                loadingAt, loadingAt.plusHours(8), 380
        );
        return cargoRepository.save(cargo).getId();
    }
}
