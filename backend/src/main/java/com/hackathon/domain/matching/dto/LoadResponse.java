package com.hackathon.domain.matching.dto;

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
}
