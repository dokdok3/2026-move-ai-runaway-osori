package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.driver.dto.RegionPoint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 보낸 필드만 반영하는 부분 수정 요청. 전부 nullable. */
public record CargoUpdateRequest(
        RegionPoint origin,
        RegionPoint destination,
        CargoType cargoType,
        String cargoDescription,
        BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        Integer desiredFare,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm
) {
}
