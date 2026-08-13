package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.service.FareQuoteService;
import com.hackathon.domain.matching.dto.AcceptResponse;
import com.hackathon.domain.matching.dto.CompletedLoadResponse;
import com.hackathon.domain.matching.dto.CompleteResponse;
import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.entity.Assignment;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadService {

    private static final double BELOW_AVERAGE_RATIO = 0.8;

    private final CargoRepository cargoRepository;
    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;
    private final AssignmentRepository assignmentRepository;
    private final MatchScoreCalculator scoreCalculator;
    private final FareQuoteService fareQuoteService;
    private final AiRouteReranker aiRouteReranker;
    private final PostgisCandidateRepository postgisCandidateRepository;

    @Transactional(readOnly = true)
    public List<LoadResponse> findAvailableLoads(Long driverId, String preferenceText, boolean refresh) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        List<DriverRoutePreference> preferences = preferenceRepository.findByDriverId(driverId);
        LocalDateTime now = LocalDateTime.now();

        List<LoadResponse> postgisLoads = findPostgisLoads(driverId);
        if (postgisLoads != null) {
            List<LoadResponse> decorated = postgisLoads.stream()
                    .map(load -> applyBelowAverageBadge(load, cargoRepository.getReferenceById(load.cargoId())))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            AiRouteReranker.RerankResult result = aiRouteReranker.rerank(driverId, preferenceText, decorated, refresh);
            if (!result.applied() && org.springframework.util.StringUtils.hasText(preferenceText)) {
                return result.loads().stream().map(load -> load.withRankingMode("RULE_FALLBACK")).toList();
            }
            return applyBestMatchBadge(result.loads());
        }

        List<LoadResponse> loads = new ArrayList<>();
        for (Cargo cargo : cargoRepository.findByStatus(CargoStatus.REQUESTED)) {
            if (!scoreCalculator.isEligible(cargo, driver, preferences, now)) {
                continue;
            }
            LoadResponse load = LoadResponse.of(cargo,
                    scoreCalculator.calculateScore(cargo, driver, preferences, now));
            loads.add(applyBelowAverageBadge(load, cargo));
        }

        loads.sort(Comparator.comparingInt(LoadResponse::matchScore).reversed()
                .thenComparing(LoadResponse::fare, Comparator.reverseOrder())
                .thenComparing(LoadResponse::cargoId));
        // BELOW_AVERAGE가 이미 붙어 있으면 그대로 둔다 — 기사에게 불리한 정보를 우선 보여준다.
        if (!loads.isEmpty() && loads.get(0).badge() == null) {
            loads.set(0, loads.get(0).withBadge("BEST_MATCH"));
        }
        return loads;
    }

    private List<LoadResponse> applyBestMatchBadge(List<LoadResponse> loads) {
        if (loads.isEmpty() || loads.get(0).badge() != null) {
            return loads;
        }
        List<LoadResponse> result = new ArrayList<>(loads);
        result.set(0, result.get(0).withBadge("BEST_MATCH"));
        return result;
    }

    private List<LoadResponse> findPostgisLoads(Long driverId) {
        try {
            List<PostgisCandidateRepository.PostgisCandidate> candidates = postgisCandidateRepository.findTop50(driverId);
            Map<Long, Cargo> cargos = cargoRepository.findAllById(candidates.stream()
                            .map(PostgisCandidateRepository.PostgisCandidate::cargoId).toList())
                    .stream().collect(java.util.stream.Collectors.toMap(Cargo::getId, Function.identity()));
            return candidates.stream().map(candidate -> {
                Cargo cargo = cargos.get(candidate.cargoId());
                if (cargo == null) {
                    throw new IllegalStateException("PostGIS 후보 화물을 찾을 수 없습니다.");
                }
                return LoadResponse.of(cargo, (int) Math.round(candidate.baseScore()))
                        .withPostgisMetrics(candidate.pickupDistanceM(), candidate.destinationGapM(), candidate.baseScore());
            }).toList();
        } catch (org.springframework.dao.DataAccessException e) {
            log.info("PostGIS 후보 검색을 사용할 수 없어 룰 기반 목록으로 폴백합니다.");
            return null;
        }
    }

    @Transactional
    public AcceptResponse accept(Long driverId, Long cargoId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_FOUND));

        if (driver.getCapacityTon().compareTo(cargo.getWeightTon()) < 0) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_SUITABLE);
        }

        int updated = cargoRepository.updateStatusIf(cargoId, CargoStatus.REQUESTED, CargoStatus.MATCHED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CARGO_ALREADY_MATCHED);
        }

        Assignment assignment = assignmentRepository.save(Assignment.of(cargoId, driverId));
        return new AcceptResponse(assignment.getId(), cargoId, CargoStatus.MATCHED.name());
    }

    @Transactional
    public CompleteResponse complete(Long driverId, Long cargoId) {
        driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_FOUND));
        Assignment assignment = assignmentRepository.findByCargoId(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_ASSIGNED));

        if (!assignment.getDriverId().equals(driverId)) {
            throw new BusinessException(ErrorCode.CARGO_ASSIGNMENT_ACCESS_DENIED);
        }
        if (cargo.getStatus() == CargoStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CARGO_ALREADY_COMPLETED);
        }
        if (cargo.getStatus() != CargoStatus.MATCHED) {
            throw new BusinessException(ErrorCode.CARGO_NOT_COMPLETABLE);
        }

        int updated = cargoRepository.updateStatusIf(cargoId, CargoStatus.MATCHED, CargoStatus.COMPLETED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CARGO_ALREADY_COMPLETED);
        }

        assignment.complete();
        Assignment completedAssignment = assignmentRepository.save(assignment);
        return new CompleteResponse(
                completedAssignment.getId(),
                cargoId,
                CargoStatus.COMPLETED.name(),
                completedAssignment.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CompletedLoadResponse> findCompletedLoads(Long driverId) {
        driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));

        List<Assignment> assignments = assignmentRepository
                .findByDriverIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(driverId);
        Map<Long, Cargo> cargos = cargoRepository.findAllById(
                        assignments.stream().map(Assignment::getCargoId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Cargo::getId, Function.identity()));

        return assignments.stream()
                .filter(assignment -> {
                    Cargo cargo = cargos.get(assignment.getCargoId());
                    return cargo != null && cargo.getStatus() == CargoStatus.COMPLETED;
                })
                .map(assignment -> CompletedLoadResponse.of(
                        assignment, cargos.get(assignment.getCargoId())))
                .toList();
    }

    /**
     * fare < averageFare * 0.8이면 BELOW_AVERAGE 배지를 붙인다.
     * AI(시세 추정) 미연동/장애 시에도 목록 자체는 막지 않는다 — 실패하면 배지 없이 그대로 둔다.
     */
    private LoadResponse applyBelowAverageBadge(LoadResponse load, Cargo cargo) {
        FareQuoteResponse fare;
        try {
            fare = fareQuoteService.quote(cargo.getOriginSido(), cargo.getDestSido(),
                    cargo.getCargoType(), cargo.getWeightTon(), cargo.getDesiredFare());
        } catch (Exception e) {
            log.warn("시세 조회 실패 - 배지 없이 응답한다: {}", e.getMessage());
            return load;
        }

        Integer averageFare = fare.averageFare();
        Integer desiredFare = cargo.getDesiredFare();
        if (averageFare == null || averageFare <= 0 || desiredFare == null
                || desiredFare >= averageFare * BELOW_AVERAGE_RATIO) {
            return load;
        }

        int belowPercent = (int) Math.round((averageFare - desiredFare) * 100.0 / averageFare);
        return load.withBelowAverage(averageFare, belowPercent);
    }
}
