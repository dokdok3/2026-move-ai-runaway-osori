package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 화물↔기사 매칭 점수를 룰 기반으로 계산한다.
 * 외부 API 호출이 없어 목록 응답이 빠르고, 점수 근거를 그대로 설명할 수 있다.
 */
@Component
public class MatchScoreCalculator {

    private static final int DIRECTION_EXACT = 20;
    private static final int DIRECTION_SIDO_ONLY = 12;
    private static final int VEHICLE_FIT = 25;
    private static final int FARE_MAX = 15;

    /** 목록에 노출할 화물인지 판정한다. */
    public boolean isEligible(Cargo cargo, Driver driver,
                              List<DriverRoutePreference> preferences, LocalDateTime now) {
        if (cargo.getLoadingAt() == null || !cargo.getLoadingAt().isAfter(now)) {
            return false;
        }
        if (driver.getCapacityTon().compareTo(cargo.getWeightTon()) < 0) {
            return false;
        }
        int origin = directionPoint(cargo.getOriginSido(), cargo.getOriginSigungu(),
                preferences, Direction.ORIGIN);
        int destination = directionPoint(cargo.getDestSido(), cargo.getDestSigungu(),
                preferences, Direction.DESTINATION);
        return origin + destination > 0;
    }

    /** 0~100 매칭 점수. isEligible이 true인 화물에만 호출한다. */
    public int calculateScore(Cargo cargo, Driver driver,
                              List<DriverRoutePreference> preferences, LocalDateTime now) {
        int route = directionPoint(cargo.getOriginSido(), cargo.getOriginSigungu(),
                preferences, Direction.ORIGIN)
                + directionPoint(cargo.getDestSido(), cargo.getDestSigungu(),
                preferences, Direction.DESTINATION);

        int vehicle = driver.getCapacityTon().compareTo(cargo.getWeightTon()) >= 0 ? VEHICLE_FIT : 0;

        return route + vehicle + timeSlackPoint(cargo.getLoadingAt(), now)
                + farePoint(cargo.getDesiredFare(), driver.getMinAcceptFare());
    }

    private int directionPoint(String sido, String sigungu,
                               List<DriverRoutePreference> preferences, Direction direction) {
        List<DriverRoutePreference> sameSido = preferences.stream()
                .filter(p -> p.getDirection() == direction)
                .filter(p -> Objects.equals(p.getSido(), sido))
                .toList();

        if (sameSido.isEmpty()) {
            return 0;
        }
        boolean exact = sameSido.stream()
                .anyMatch(p -> p.getSigungu() == null || Objects.equals(p.getSigungu(), sigungu));
        return exact ? DIRECTION_EXACT : DIRECTION_SIDO_ONLY;
    }

    private int timeSlackPoint(LocalDateTime loadingAt, LocalDateTime now) {
        long hours = Duration.between(now, loadingAt).toHours();
        if (hours >= 24) {
            return 20;
        }
        if (hours >= 12) {
            return 12;
        }
        return 5;
    }

    private int farePoint(Integer fare, Integer minAcceptFare) {
        if (minAcceptFare == null || minAcceptFare <= 0 || fare >= minAcceptFare) {
            return FARE_MAX;
        }
        return (int) Math.floor((double) FARE_MAX * fare / minAcceptFare);
    }
}
