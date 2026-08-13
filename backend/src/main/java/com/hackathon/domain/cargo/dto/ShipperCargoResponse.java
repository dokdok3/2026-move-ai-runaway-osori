package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.driver.dto.RegionPoint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 화주가 등록한 화물 목록에 필요한 요약 정보. 목록 조회에서는 AI 시세를 재조회하지 않는다. */
public record ShipperCargoResponse(
        Long cargoId,
        String status,
        RegionPoint origin,
        RegionPoint destination,
        String cargoType,
        String cargoDescription,
        BigDecimal weightTon,
        Integer desiredFare,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        LocalDateTime createdAt
) {
    public static ShipperCargoResponse from(Cargo cargo) {
        return new ShipperCargoResponse(
                cargo.getId(),
                cargo.getStatus().name(),
                new RegionPoint(cargo.getOriginSido(), cargo.getOriginSigungu()),
                new RegionPoint(cargo.getDestSido(), cargo.getDestSigungu()),
                cargo.getCargoType() != null ? cargo.getCargoType().name() : null,
                cargo.getCargoDescription(),
                cargo.getWeightTon(),
                cargo.getDesiredFare(),
                cargo.getLoadingAt(),
                cargo.getUnloadingAt(),
                cargo.getCreatedAt()
        );
    }
}
