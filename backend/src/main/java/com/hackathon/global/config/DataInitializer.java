package com.hackathon.global.config;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import com.hackathon.domain.shipper.entity.Shipper;
import com.hackathon.domain.shipper.repository.ShipperRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 해커톤 데모용 seed 데이터. 기사 회원가입 화면이 없으므로(범위 밖) 여기서 driverId=1,2를 미리 만들어둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;
    private final CargoRepository cargoRepository;
    private final ShipperRepository shipperRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (shipperRepository.count() == 0) {
            shipperRepository.save(Shipper.create(1L));
        }

        if (driverRepository.count() > 0) {
            return;
        }

        // driverId = 1 — 기사 화면의 로그인 사용자
        Driver me = driverRepository.save(Driver.create(
                "박OO", "010-1111-2222", "34나 5678",
                "카고", new BigDecimal("25.0"), "윙바디", "GENERAL|REFRIGERATED",
                new BigDecimal("4.6"), 87, 95, 150_000,
                "06:00", "20:00", "수원→부산 · 3일 전"));

        preferenceRepository.saveAll(List.of(
                DriverRoutePreference.of(me.getId(), Direction.ORIGIN, "경기도", "수원시"),
                DriverRoutePreference.of(me.getId(), Direction.ORIGIN, "경기도", "용인시"),
                DriverRoutePreference.of(me.getId(), Direction.ORIGIN, "서울특별시", null),
                DriverRoutePreference.of(me.getId(), Direction.DESTINATION, "부산광역시", null),
                DriverRoutePreference.of(me.getId(), Direction.DESTINATION, "서울특별시", "송파구")
        ));

        // driverId = 2 — 화주 화면 "배차된 기사" 카드용, 적재량이 작아 초과 화물 제외 케이스에도 쓰인다
        driverRepository.save(Driver.create(
                "김OO", "010-3333-4444", "12가 3456",
                "탑차", new BigDecimal("5.0"), "냉장", "REFRIGERATED",
                new BigDecimal("4.8"), 132, 98, 450_000,
                "06:00", "20:00", "서울→대구 · 2일 전"));

        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
                .withSecond(0).withNano(0);

        cargoRepository.saveAll(List.of(
                // driverId=1의 구간에 딱 맞는 화물 — BEST_MATCH 후보
                Cargo.create(100L, "경기도", "수원시", "부산광역시", "강서구",
                        CargoType.REFRIGERATED, "냉장식품", new BigDecimal("22.0"), "카고", "윙바디",
                        850_000, base.plusDays(1), base.plusDays(1).plusHours(9), 380),
                // 서울(전체) → 송파구, 최소수락운임 미달 — 운임매력 감점 후보
                Cargo.create(101L, "서울특별시", "강남구", "서울특별시", "송파구",
                        CargoType.GENERAL, "생활용품", new BigDecimal("5.0"), "탑차", null,
                        95_000, base.plusHours(10), base.plusHours(13), 45),
                // 구간 불일치 — 목록에서 제외되어야 함
                Cargo.create(102L, "광주광역시", "광산구", "전라북도", "전주시",
                        CargoType.GENERAL, "일반화물", new BigDecimal("11.0"), "카고", null,
                        210_000, base.plusDays(2), base.plusDays(2).plusHours(3), 100),
                // 적재량 초과 — 25t 기사에게는 보이지만 5t 기사에게는 제외
                Cargo.create(103L, "경기도", "용인시", "부산광역시", "해운대구",
                        CargoType.GENERAL, "산업자재", new BigDecimal("30.0"), "카고", null,
                        1_200_000, base.plusDays(2), base.plusDays(2).plusHours(8), 390)
        ));

        log.info("seed 기사 {}명, 화물 {}건 생성", driverRepository.count(), cargoRepository.count());
    }
}
