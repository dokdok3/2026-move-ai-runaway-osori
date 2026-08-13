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
import com.hackathon.domain.matching.dto.NextLoadRecommendationResponse;
import com.hackathon.domain.matching.entity.Assignment;
import com.hackathon.domain.matching.entity.DriverHiddenCargo;
import com.hackathon.domain.matching.entity.DriverHiddenCargoId;
import com.hackathon.domain.matching.entity.LoadType;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.domain.matching.repository.DriverHiddenCargoRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
    private final DriverHiddenCargoRepository driverHiddenCargoRepository;
    private final CargoCompletionProcessor cargoCompletionProcessor;
    private final ConnectionRecommendationService connectionRecommendationService;

    @Transactional(readOnly = true)
    public List<LoadResponse> findLoads(Long driverId, LoadType filter, String preferenceText, boolean refresh) {
        return switch (filter) {
            case ACCEPTED -> findAcceptedLoads(driverId);
            case HIDDEN -> findHiddenLoads(driverId);
            case ALL -> findAvailableLoads(driverId, preferenceText, refresh);
        };
    }

    private List<LoadResponse> findAcceptedLoads(Long driverId) {
        List<Long> cargoIds = assignmentRepository.findByDriverIdOrderByAcceptedAtDesc(driverId).stream()
                .map(Assignment::getCargoId)
                .toList();
        return loadResponsesFor(cargoIds);
    }

    private List<LoadResponse> findHiddenLoads(Long driverId) {
        List<Long> cargoIds = driverHiddenCargoRepository.findByDriverIdOrderByHiddenAtDesc(driverId).stream()
                .map(DriverHiddenCargo::getCargoId)
                .toList();
        return loadResponsesFor(cargoIds);
    }

    private List<LoadResponse> loadResponsesFor(List<Long> cargoIds) {
        Map<Long, Cargo> cargos = cargoRepository.findAllById(cargoIds).stream()
                .collect(java.util.stream.Collectors.toMap(Cargo::getId, Function.identity()));
        return cargoIds.stream()
                       .map(cargos::get)
                       .filter(Objects::nonNull)
                       .map(cargo -> LoadResponse.of(cargo, 0))
                       .toList();
    }

    private List<LoadResponse> findAvailableLoads(Long driverId, String preferenceText, boolean refresh) {
        Driver driver = driverRepository.findById(driverId)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        List<DriverRoutePreference> preferences = preferenceRepository.findByDriverId(driverId);
        LocalDateTime now = LocalDateTime.now();

        List<LoadResponse> postgisLoads = findPostgisLoads(driverId);
        if (postgisLoads != null) {
            List<LoadResponse> decorated = postgisLoads.stream()
                    .map(load -> applyFareInfo(load, cargoRepository.getReferenceById(load.cargoId())))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            AiRouteReranker.RerankResult result = aiRouteReranker.rerank(driverId, preferenceText, decorated, refresh);
            if (!result.applied() && org.springframework.util.StringUtils.hasText(preferenceText)) {
                return result.loads().stream().map(load -> load.withRankingMode("RULE_FALLBACK")).toList();
            }
            return result.loads();
        }

        Set<Long> hiddenCargoIds = driverHiddenCargoRepository.findByDriverIdOrderByHiddenAtDesc(driverId)
                                                              .stream()
                                                              .map(DriverHiddenCargo::getCargoId)
                                                              .collect(Collectors.toSet());
        List<LoadResponse> loads = new ArrayList<>();
        for (Cargo cargo : cargoRepository.findByStatus(CargoStatus.REQUESTED)) {
            if (hiddenCargoIds.contains(cargo.getId()) || !scoreCalculator.isEligible(cargo, driver, preferences, now)) {
                continue;
            }
            LoadResponse load = LoadResponse.of(cargo,
                    scoreCalculator.calculateScore(cargo, driver, preferences, now));
            loads.add(applyFareInfo(load, cargo));
        }

        loads.sort(Comparator.comparingInt(LoadResponse::matchScore).reversed()
                .thenComparing(LoadResponse::fare, Comparator.reverseOrder())
                .thenComparing(LoadResponse::cargoId));
        return loads;
    }

    private List<LoadResponse> findPostgisLoads(Long driverId) {
        try {
            List<PostgisCandidateRepository.PostgisCandidate> candidates = postgisCandidateRepository.findTop50(driverId);
            Map<Long, Cargo> cargos = cargoRepository.findAllById(candidates.stream()
                            .map(PostgisCandidateRepository.PostgisCandidate::cargoId).toList())
                    .stream().collect(Collectors.toMap(Cargo::getId, Function.identity()));
            return candidates.stream().map(it -> {
                Cargo cargo = cargos.get(it.cargoId());
                if (cargo == null) {
                    throw new IllegalStateException("PostGIS 후보 화물을 찾을 수 없습니다.");
                }
                return LoadResponse.of(cargo, (int) Math.round(it.baseScore()))
                        .withPostgisMetrics(it.pickupDistanceM(), it.destinationGapM(), it.baseScore());
            }).toList();
        } catch (DataAccessException e) {
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

        DriverHiddenCargoId hiddenId = new DriverHiddenCargoId(driverId, cargoId);
        if (driverHiddenCargoRepository.existsById(hiddenId)) {
            driverHiddenCargoRepository.deleteById(hiddenId);
        }
        Assignment assignment = assignmentRepository.save(Assignment.of(cargoId, driverId));
        return new AcceptResponse(assignment.getId(), cargoId, CargoStatus.MATCHED.name());
    }

    public CompleteResponse complete(Long driverId, Long cargoId) {
        CargoCompletionProcessor.CompletionResult completion =
                cargoCompletionProcessor.complete(driverId, cargoId);
        return new CompleteResponse(
                completion.assignmentId(),
                completion.cargoId(),
                CargoStatus.COMPLETED.name(),
                completion.completedAt(),
                connectionRecommendationOrNull(driverId, cargoId, completion.completedAt())
        );
    }

    private NextLoadRecommendationResponse connectionRecommendationOrNull(
            Long driverId,
            Long cargoId,
            LocalDateTime completedAt
    ) {
        try {
            return connectionRecommendationService.recommend(driverId, cargoId, completedAt);
        } catch (Exception e) {
            log.warn("연계 배차 추천 실패 - 배송 완료 결과만 반환합니다. cargoId={}", cargoId, e);
            return null;
        }
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

    @Transactional
    public void hide(Long driverId, Long cargoId) {
        if (!driverRepository.existsById(driverId)) {
            throw new BusinessException(ErrorCode.DRIVER_NOT_FOUND);
        }
        if (!cargoRepository.existsById(cargoId)) {
            throw new BusinessException(ErrorCode.CARGO_NOT_FOUND);
        }
        DriverHiddenCargoId id = new DriverHiddenCargoId(driverId, cargoId);
        if (!driverHiddenCargoRepository.existsById(id)) {
            driverHiddenCargoRepository.save(DriverHiddenCargo.of(driverId, cargoId));
        }
    }

    /**
     * 시세 조회에 성공하면 fare < averageFare * 0.8 여부에 따라 LOW/FAIR 판정을 붙인다.
     * AI(시세 추정) 미연동/장애 시에도 목록 자체는 막지 않는다 — 실패할 때만 시세 정보 없이 반환한다.
     */
    private LoadResponse applyFareInfo(LoadResponse load, Cargo cargo) {
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
        if (averageFare == null || averageFare <= 0 || desiredFare == null) {
            return load;
        }

        if (desiredFare >= averageFare * BELOW_AVERAGE_RATIO) {
            return load.withFareInfo("FAIR", averageFare, null);
        }
        int belowPercent = (int) Math.round((averageFare - desiredFare) * 100.0 / averageFare);
        return load.withFareInfo("LOW", averageFare, belowPercent);
    }
}
