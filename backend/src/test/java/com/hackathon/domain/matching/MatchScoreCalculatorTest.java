package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.matching.service.MatchScoreCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchScoreCalculatorTest {

    private final MatchScoreCalculator calculator = new MatchScoreCalculator();
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 11, 9, 0);

    private Driver driver() {
        return Driver.create("박OO", "010", "34나 5678", "카고",
                new BigDecimal("25.0"), "윙바디", "GENERAL|REFRIGERATED", new BigDecimal("4.6"),
                87, 95, 150_000, "06:00", "20:00", "최근 운행");
    }

    private List<DriverRoutePreference> preferences() {
        return List.of(
                DriverRoutePreference.of(1L, Direction.ORIGIN, "경기도", "수원시"),
                DriverRoutePreference.of(1L, Direction.ORIGIN, "서울특별시", null),
                DriverRoutePreference.of(1L, Direction.DESTINATION, "부산광역시", null),
                DriverRoutePreference.of(1L, Direction.DESTINATION, "서울특별시", "송파구"));
    }

    private Cargo cargo(String oSido, String oSigungu, String dSido, String dSigungu,
                        String weightTon, int fare, LocalDateTime loadingAt) {
        return Cargo.create(1L, oSido, oSigungu, dSido, dSigungu,
                CargoType.GENERAL, "일반화물", new BigDecimal(weightTon), "카고", null,
                fare, loadingAt, loadingAt.plusHours(4), 100);
    }

    @Test
    @DisplayName("설계 문서 검산 1: 수원→부산(전체), 36시간 뒤, 운임 충분 = 100점")
    void perfectMatch() {
        Cargo cargo = cargo("경기도", "수원시", "부산광역시", "강서구",
                "22.0", 850_000, now.plusHours(36));

        int score = calculator.calculateScore(cargo, driver(), preferences(), now);

        assertThat(score).isEqualTo(100); // 20 + 20 + 25 + 20 + 15
    }

    @Test
    @DisplayName("설계 문서 검산 2: 서울(전체)→송파구, 10시간 뒤, 운임 미달 = 79점")
    void belowMinAcceptFare() {
        Cargo cargo = cargo("서울특별시", "강남구", "서울특별시", "송파구",
                "5.0", 95_000, now.plusHours(10));

        int score = calculator.calculateScore(cargo, driver(), preferences(), now);

        // 20(서울 전체) + 20(송파구 정확) + 25 + 5(10시간) + 9(floor(15*95000/150000))
        assertThat(score).isEqualTo(79);
    }

    @Test
    @DisplayName("시도만 일치하면 방향당 12점")
    void sidoOnlyMatch() {
        Cargo cargo = cargo("경기도", "평택시", "부산광역시", "강서구",
                "10.0", 900_000, now.plusHours(36));

        int score = calculator.calculateScore(cargo, driver(), preferences(), now);

        assertThat(score).isEqualTo(92); // 12 + 20 + 25 + 20 + 15
    }

    @Test
    @DisplayName("적재량이 부족하면 목록에서 제외")
    void excludesOverweight() {
        Cargo cargo = cargo("경기도", "수원시", "부산광역시", "강서구",
                "30.0", 900_000, now.plusHours(36));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isFalse();
    }

    @Test
    @DisplayName("출발·도착 모두 구간 밖이면 제외")
    void excludesUnmatchedRoute() {
        Cargo cargo = cargo("광주광역시", "광산구", "전라북도", "전주시",
                "10.0", 900_000, now.plusHours(36));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isFalse();
    }

    @Test
    @DisplayName("상차 시각이 지난 화물은 제외")
    void excludesPastLoadingTime() {
        Cargo cargo = cargo("경기도", "수원시", "부산광역시", "강서구",
                "10.0", 900_000, now.minusHours(1));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isFalse();
    }

    @Test
    @DisplayName("한쪽 방향만 맞으면 목록에는 남는다")
    void keepsPartialMatch() {
        Cargo cargo = cargo("경기도", "수원시", "제주특별자치도", "제주시",
                "10.0", 900_000, now.plusHours(36));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isTrue();
        assertThat(calculator.calculateScore(cargo, driver(), preferences(), now))
                .isEqualTo(80); // 20 + 0 + 25 + 20 + 15
    }
}
