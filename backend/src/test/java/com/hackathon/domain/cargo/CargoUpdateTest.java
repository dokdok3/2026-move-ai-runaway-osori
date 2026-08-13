package com.hackathon.domain.cargo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.repository.CargoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class CargoUpdateTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CargoRepository cargoRepository;

    private Long newCargo() {
        return cargoRepository.save(Cargo.create(200L,
                "경기도", "수원시", "부산광역시", "강서구",
                CargoType.GENERAL, "일반화물", new BigDecimal("3.0"), "카고", null,
                500_000, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(8), 380
        )).getId();
    }

    @Test
    @DisplayName("보낸 필드만 반영해 운임을 수정한다")
    void updatesFareOnly() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredFare\": 620000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.desiredFare").value(620000))
                .andExpect(jsonPath("$.data.origin.sido").value("경기도"));
    }

    @Test
    @DisplayName("주소를 수정할 때 시군구가 없으면 400")
    void rejectsMissingSigunguOnRouteUpdate() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": {"sido": "서울특별시", "sigungu": null},
                                  "destination": {"sido": "부산광역시"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("시군구는 필수입니다."));

        Cargo cargo = cargoRepository.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(cargo.getOriginSigungu()).isEqualTo("수원시");
        org.assertj.core.api.Assertions.assertThat(cargo.getDestSigungu()).isEqualTo("강서구");
    }

    @Test
    @DisplayName("주소를 수정할 때 시군구가 전체이면 400")
    void rejectsWholeSigunguOnRouteUpdate() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin": {"sido": "서울특별시", "sigungu": "전체"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("시군구는 필수입니다."));

        Cargo cargo = cargoRepository.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(cargo.getOriginSigungu()).isEqualTo("수원시");
    }

    @Test
    @Transactional
    @DisplayName("배차 완료된 화물은 수정할 수 없다")
    void rejectsMatchedCargo() throws Exception {
        Long id = newCargo();
        cargoRepository.updateStatusIf(id, CargoStatus.REQUESTED, CargoStatus.MATCHED);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredFare\": 620000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 화주의 화물은 수정할 수 없다")
    void rejectsUpdateByOtherShipper() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredFare\": 620000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("배차 대기 중인 내 화물을 삭제한다")
    void deletesMyRequestedCargo() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cargos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 화주의 화물은 삭제할 수 없다")
    void rejectsDeleteByOtherShipper() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/cargos/" + id)
                        .header("X-User-Id", "201"))
                .andExpect(status().isForbidden());
    }
}
