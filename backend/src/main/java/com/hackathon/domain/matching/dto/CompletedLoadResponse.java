package com.hackathon.domain.matching.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.matching.entity.Assignment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompletedLoadResponse(
        Long assignmentId,
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
        String status,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt
) {
    public static CompletedLoadResponse of(Assignment assignment, Cargo cargo) {
        return new CompletedLoadResponse(
                assignment.getId(),
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
                cargo.getStatus().name(),
                assignment.getAcceptedAt(),
                assignment.getCompletedAt()
        );
    }

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
