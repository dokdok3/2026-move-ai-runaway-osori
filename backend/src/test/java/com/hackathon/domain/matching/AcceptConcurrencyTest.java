package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.domain.matching.service.LoadService;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AcceptConcurrencyTest {

    @Autowired
    LoadService loadService;

    @Autowired
    CargoRepository cargoRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    private Long newCargo() {
        return cargoRepository.save(Cargo.create(300L,
                "경기도", "수원시", "부산광역시", "강서구",
                CargoType.GENERAL, "일반화물", new BigDecimal("3.0"), "카고", null,
                800_000, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(8), 380
        )).getId();
    }

    @Test
    @DisplayName("두 기사가 동시에 수락하면 한 명만 성공한다")
    void onlyOneDriverWins() throws Exception {
        Long cargoId = newCargo();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (long driverId : new long[]{1L, 2L}) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    loadService.accept(driverId, cargoId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.CARGO_ALREADY_MATCHED) {
                        conflict.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.MATCHED);
        assertThat(assignmentRepository.findByCargoId(cargoId)).isPresent();
    }

    @Test
    @DisplayName("적재량이 부족한 기사는 수락할 수 없다")
    void rejectsOverweight() {
        Long cargoId = cargoRepository.save(Cargo.create(300L,
                "경기도", "수원시", "부산광역시", "강서구",
                CargoType.GENERAL, "일반화물", new BigDecimal("20.0"), "카고", null,
                800_000, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(8), 380
        )).getId();

        // driverId=2는 5t 탑차
        assertThatThrownBy(() -> loadService.accept(2L, cargoId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_SUITABLE);
    }
}
