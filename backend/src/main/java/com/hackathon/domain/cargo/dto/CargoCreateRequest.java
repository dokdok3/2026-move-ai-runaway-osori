package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.driver.dto.RegionPoint;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CargoCreateRequest(
        @NotNull(message = "출발지를 입력해주세요.") RegionPoint origin,
        @NotNull(message = "도착지를 입력해주세요.") RegionPoint destination,
        @NotNull(message = "화물 종류를 입력해주세요.") CargoType cargoType,
        String cargoDescription,
        @NotNull(message = "중량을 입력해주세요.") BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        @NotNull(message = "희망 운임을 입력해주세요.") Integer desiredFare,
        @NotNull(message = "상차 일시를 입력해주세요.") LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm
) {
}
