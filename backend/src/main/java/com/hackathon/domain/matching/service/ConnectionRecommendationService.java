package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.matching.dto.NextLoadRecommendationResponse;
import com.hackathon.domain.matching.entity.DriverHiddenCargo;
import com.hackathon.domain.matching.repository.DriverHiddenCargoRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionRecommendationService {

    private static final long MAX_EMPTY_DISTANCE_M = 100_000;
    private static final long MAX_WAIT_MINUTES = 48 * 60;
    private static final long LOADING_PREPARATION_MINUTES = 30;

    private final CargoRepository cargoRepository;
    private final DriverRepository driverRepository;
    private final DriverHiddenCargoRepository hiddenCargoRepository;
    private final ConnectionCandidateRepository candidateRepository;
    private final AiConnectionRecommender aiConnectionRecommender;

    public NextLoadRecommendationResponse recommend(
            Long driverId,
            Long completedCargoId,
            LocalDateTime completedAt
    ) {
        Driver driver = driverRepository.findById(driverId).orElse(null);
        Cargo completedCargo = cargoRepository.findById(completedCargoId).orElse(null);
        if (driver == null || completedCargo == null) {
            return null;
        }

        LocalDateTime availableAt = completedAt;
        List<ConnectionRecommendationCandidate> candidates = findCandidates(
                completedCargo, driver, availableAt);
        if (candidates.isEmpty()) {
            return null;
        }

        AiConnectionRecommender.RecommendationDecision decision =
                aiConnectionRecommender.recommend(candidates);
        ConnectionRecommendationCandidate selected = candidates.stream()
                .filter(candidate -> candidate.cargo().getId().equals(decision.cargoId()))
                .findFirst()
                .orElse(candidates.getFirst());

        return NextLoadRecommendationResponse.of(
                selected.cargo(),
                selected.emptyDistanceKm(),
                selected.waitMinutes(),
                selected.connectionScore(),
                decision.recommendationSummary(),
                decision.reasons(),
                decision.explanationMode()
        );
    }

    private List<ConnectionRecommendationCandidate> findCandidates(
            Cargo completedCargo,
            Driver driver,
            LocalDateTime availableAt
    ) {
        try {
            return postgisCandidates(completedCargo, driver, availableAt);
        } catch (DataAccessException e) {
            log.info("PostGIS 연계 배차 검색을 사용할 수 없어 동일 지역 기반 검색으로 폴백합니다.");
            return sameRegionCandidates(completedCargo, driver, availableAt);
        }
    }

    private List<ConnectionRecommendationCandidate> postgisCandidates(
            Cargo completedCargo,
            Driver driver,
            LocalDateTime availableAt
    ) {
        List<ConnectionCandidateRepository.PostgisConnectionCandidate> found =
                candidateRepository.findCandidates(completedCargo.getId(), driver.getId(), availableAt);
        Map<Long, Cargo> cargos = cargoRepository.findAllById(found.stream()
                        .map(ConnectionCandidateRepository.PostgisConnectionCandidate::cargoId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Cargo::getId, Function.identity()));

        List<ConnectionRecommendationCandidate> candidates = new ArrayList<>();
        for (ConnectionCandidateRepository.PostgisConnectionCandidate candidate : found) {
            Cargo cargo = cargos.get(candidate.cargoId());
            if (cargo == null) {
                continue;
            }
            addIfEligible(candidates, cargo, driver, availableAt, candidate.emptyDistanceM());
        }
        return sort(candidates);
    }

    /** H2 테스트나 PostGIS 장애에서는 좌표를 추측하지 않고 동일 시군구만 공차 0km로 취급한다. */
    private List<ConnectionRecommendationCandidate> sameRegionCandidates(
            Cargo completedCargo,
            Driver driver,
            LocalDateTime availableAt
    ) {
        Set<Long> hiddenIds = hiddenCargoRepository.findByDriverIdOrderByHiddenAtDesc(driver.getId())
                .stream()
                .map(DriverHiddenCargo::getCargoId)
                .collect(Collectors.toSet());
        List<ConnectionRecommendationCandidate> candidates = new ArrayList<>();
        for (Cargo cargo : cargoRepository.findByStatus(CargoStatus.REQUESTED)) {
            if (hiddenIds.contains(cargo.getId())
                    || !Objects.equals(completedCargo.getDestSido(), cargo.getOriginSido())
                    || !Objects.equals(completedCargo.getDestSigungu(), cargo.getOriginSigungu())) {
                continue;
            }
            addIfEligible(candidates, cargo, driver, availableAt, 0L);
        }
        return sort(candidates);
    }

    private void addIfEligible(
            List<ConnectionRecommendationCandidate> result,
            Cargo cargo,
            Driver driver,
            LocalDateTime availableAt,
            long emptyDistanceM
    ) {
        if (!isEligible(cargo, driver, availableAt, emptyDistanceM)) {
            return;
        }
        long waitMinutes = Duration.between(availableAt, cargo.getLoadingAt()).toMinutes();
        double emptyDistanceKm = Math.round(emptyDistanceM / 100.0) / 10.0;
        result.add(new ConnectionRecommendationCandidate(
                cargo,
                emptyDistanceKm,
                waitMinutes,
                connectionScore(driver, cargo, emptyDistanceKm, waitMinutes)
        ));
    }

    private boolean isEligible(Cargo cargo, Driver driver, LocalDateTime availableAt, long emptyDistanceM) {
        if (cargo.getStatus() != CargoStatus.REQUESTED
                || cargo.getLoadingAt() == null
                || emptyDistanceM < 0
                || emptyDistanceM > MAX_EMPTY_DISTANCE_M
                || cargo.getWeightTon() == null
                || driver.getCapacityTon().compareTo(cargo.getWeightTon()) < 0
                || cargo.getCargoType() == null
                || !supports(driver, cargo)
                || cargo.getDesiredFare() == null
                || (driver.getMinAcceptFare() != null
                    && cargo.getDesiredFare() < driver.getMinAcceptFare())) {
            return false;
        }

        long waitMinutes = Duration.between(availableAt, cargo.getLoadingAt()).toMinutes();
        long transferMinutes = (long) Math.ceil(emptyDistanceM / 1_000.0) + LOADING_PREPARATION_MINUTES;
        return waitMinutes > 0
                && waitMinutes <= MAX_WAIT_MINUTES
                && waitMinutes >= transferMinutes;
    }

    private boolean supports(Driver driver, Cargo cargo) {
        if (driver.getVehicleCargoTypes() == null) {
            return false;
        }
        return List.of(driver.getVehicleCargoTypes().split("\\|"))
                .contains(cargo.getCargoType().name());
    }

    private int connectionScore(
            Driver driver,
            Cargo cargo,
            double emptyDistanceKm,
            long waitMinutes
    ) {
        int distanceScore = (int) Math.round(45 * (1 - emptyDistanceKm / 100.0));
        int waitScore = (int) Math.round(25 * (1 - waitMinutes / (double) MAX_WAIT_MINUTES));
        int fareScore;
        if (driver.getMinAcceptFare() == null || driver.getMinAcceptFare() <= 0) {
            fareScore = 30;
        } else {
            double fareRatio = cargo.getDesiredFare() / (double) driver.getMinAcceptFare();
            fareScore = (int) Math.round(15 * Math.min(2.0, fareRatio));
        }
        return Math.max(0, Math.min(100, distanceScore + waitScore + fareScore));
    }

    private List<ConnectionRecommendationCandidate> sort(
            List<ConnectionRecommendationCandidate> candidates
    ) {
        return candidates.stream()
                .sorted(Comparator.comparingInt(ConnectionRecommendationCandidate::connectionScore).reversed()
                        .thenComparingDouble(ConnectionRecommendationCandidate::emptyDistanceKm)
                        .thenComparing(candidate -> candidate.cargo().getDesiredFare(), Comparator.reverseOrder())
                        .thenComparing(candidate -> candidate.cargo().getId()))
                .toList();
    }
}
