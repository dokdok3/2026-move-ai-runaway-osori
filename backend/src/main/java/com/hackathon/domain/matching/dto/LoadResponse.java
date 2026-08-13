package com.hackathon.domain.matching.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoadResponse(
        Long cargoId,
        String origin,
        String destination,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm,
        String vehicleType,
        BigDecimal weightTon,
        String bodyType,
        String cargoType,
        Integer fare,
        Integer matchScore,
        String badge,
        Integer regionAverageFare,
        Integer belowPercent
) {
    public static LoadResponse of(Cargo cargo, int matchScore) {
        return new LoadResponse(
                cargo.getId(),
                label(cargo.getOriginSido(), cargo.getOriginSigungu()),
                label(cargo.getDestSido(), cargo.getDestSigungu()),
                cargo.getLoadingAt(),
                cargo.getUnloadingAt(),
                cargo.getDistanceKm(),
                cargo.getVehicleType(),
                cargo.getWeightTon(),
                cargo.getBodyType(),
                cargo.getCargoType() != null ? cargo.getCargoType().name() : null,
                cargo.getDesiredFare(),
                matchScore,
                null, null, null
        );
    }

    public LoadResponse withBadge(String badge) {
        return new LoadResponse(cargoId, origin, destination, loadingAt, unloadingAt,
                distanceKm, vehicleType, weightTon, bodyType, cargoType, fare,
                matchScore, badge, regionAverageFare, belowPercent);
    }

    public LoadResponse withBelowAverage(Integer regionAverageFare, Integer belowPercent) {
        return new LoadResponse(cargoId, origin, destination, loadingAt, unloadingAt,
                distanceKm, vehicleType, weightTon, bodyType, cargoType, fare,
                matchScore, "BELOW_AVERAGE", regionAverageFare, belowPercent);
    }

    /** "서울특별시 강남구" → "서울 강남구" 처럼 화면 표기용으로 시도명만 줄인다. */
    private static String label(String sido, String sigungu) {
        String shortSido = sido
                .replace("특별자치도", "")
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("자치도", "")
                .replace("도", "");
        return sigungu == null ? shortSido : shortSido + " " + sigungu;
    }
}
