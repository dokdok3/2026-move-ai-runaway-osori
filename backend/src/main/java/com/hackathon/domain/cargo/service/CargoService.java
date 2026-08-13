package com.hackathon.domain.cargo.service;

import com.hackathon.domain.cargo.dto.CargoCreateRequest;
import com.hackathon.domain.cargo.dto.CargoDetailResponse;
import com.hackathon.domain.cargo.dto.CargoUpdateRequest;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.domain.fare.service.FareQuoteService;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CargoService {

    private final CargoRepository cargoRepository;
    private final AssignmentRepository assignmentRepository;
    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;
    private final FareQuoteService fareQuoteService;

    @Transactional
    public Long create(Long shipperId, CargoCreateRequest request) {
        Cargo cargo = request.toEntity(shipperId);
        Cargo saved = cargoRepository.save(cargo);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public CargoDetailResponse findDetail(Long cargoId) {
        Cargo cargo = getCargo(cargoId);
        return CargoDetailResponse.of(cargo, quoteOrNull(cargo), assignedDriverOrNull(cargoId));
    }

    @Transactional
    public CargoDetailResponse update(Long cargoId, CargoUpdateRequest request) {
        Cargo cargo = getCargo(cargoId);
        if (!cargo.isModifiable()) {
            throw new BusinessException(ErrorCode.CARGO_NOT_MODIFIABLE);
        }

        if (request.origin() != null || request.destination() != null) {
            cargo.changeRoute(
                    request.origin() != null ? request.origin().sido() : cargo.getOriginSido(),
                    request.origin() != null ? request.origin().sigungu() : cargo.getOriginSigungu(),
                    request.destination() != null ? request.destination().sido() : cargo.getDestSido(),
                    request.destination() != null ? request.destination().sigungu() : cargo.getDestSigungu(),
                    request.distanceKm());
        }
        if (request.cargoType() != null || request.cargoDescription() != null
                || request.weightTon() != null || request.vehicleType() != null || request.bodyType() != null) {
            cargo.changeCargoSpec(request.cargoType(), request.cargoDescription(),
                    request.weightTon(), request.vehicleType(), request.bodyType());
        }
        if (request.desiredFare() != null) {
            cargo.changeFare(request.desiredFare());
        }
        if (request.loadingAt() != null || request.unloadingAt() != null) {
            cargo.changeSchedule(request.loadingAt(), request.unloadingAt());
        }

        return CargoDetailResponse.of(cargo, quoteOrNull(cargo), assignedDriverOrNull(cargoId));
    }

    @Transactional(readOnly = true)
    public Cargo getCargo(Long cargoId) {
        return cargoRepository.findById(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_FOUND));
    }

    private DriverResponse assignedDriverOrNull(Long cargoId) {
        return assignmentRepository.findByCargoId(cargoId)
                .flatMap(a -> driverRepository.findById(a.getDriverId()))
                .map(d -> DriverResponse.from(d, preferenceRepository.findByDriverId(d.getId())))
                .orElse(null);
    }

    /** AI 시세 추정이 아직 연동되지 않았거나 장애가 나도 화물 조회 자체는 막지 않는다. */
    private FareQuoteResponse quoteOrNull(Cargo cargo) {
        try {
            return fareQuoteService.quote(cargo.getOriginSido(), cargo.getDestSido(),
                    cargo.getCargoType(), cargo.getWeightTon(), cargo.getDesiredFare());
        } catch (Exception e) {
            log.warn("시세 조회 실패 - fare 없이 응답한다: {}", e.getMessage());
            return null;
        }
    }
}
