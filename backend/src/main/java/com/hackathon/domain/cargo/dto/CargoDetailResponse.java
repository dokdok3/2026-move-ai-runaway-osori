package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.dto.RegionPoint;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CargoDetailResponse(
        Long cargoId,
        String status,
        RegionPoint origin,
        RegionPoint destination,
        String cargoType,
        String cargoDescription,
        BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        Integer desiredFare,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm,
        FareQuoteResponse fare,
        DriverResponse assignedDriver
) {
    public static CargoDetailResponse of(Cargo cargo, FareQuoteResponse fare, DriverResponse driver) {
        return new CargoDetailResponse(
                cargo.getId(),
                cargo.getStatus().name(),
                new RegionPoint(cargo.getOriginSido(), cargo.getOriginSigungu()),
                new RegionPoint(cargo.getDestSido(), cargo.getDestSigungu()),
                cargo.getCargoType() != null ? cargo.getCargoType().name() : null,
                cargo.getCargoDescription(),
                cargo.getWeightTon(),
                cargo.getVehicleType(),
                cargo.getBodyType(),
                cargo.getDesiredFare(),
                cargo.getLoadingAt(),
                cargo.getUnloadingAt(),
                cargo.getDistanceKm(),
                fare,
                driver
        );
    }
}
