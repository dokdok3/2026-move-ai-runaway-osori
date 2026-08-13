package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CargoCreateRequest(
        @NotNull(message = "출발지는 필수입니다.") @Valid CargoRegionPoint origin,
        @NotNull(message = "도착지는 필수입니다.") @Valid CargoRegionPoint destination,
        @NotNull(message = "화물 종류는 필수입니다.") CargoType cargoType,
        String cargoDescription,
        @NotNull(message = "화물 중량은 필수입니다.") BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        @NotNull(message = "희망 운임은 필수입니다.") Integer desiredFare,
        @NotNull(message = "상차 일시는 필수입니다.") LocalDateTime loadingAt,
        @NotNull(message = "하차 일시는 필수입니다.") LocalDateTime unloadingAt,
        Integer distanceKm
) {
    public Cargo toEntity(Long shipperId) {
        return Cargo.create(
                shipperId,
                origin.sido(), origin.sigungu(),
                destination.sido(), destination.sigungu(),
                cargoType, cargoDescription, weightTon,
                vehicleType, bodyType,
                desiredFare,
                loadingAt, unloadingAt,
                distanceKm);
    }
}
