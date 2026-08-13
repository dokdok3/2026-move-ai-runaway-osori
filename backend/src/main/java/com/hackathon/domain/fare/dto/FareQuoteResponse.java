package com.hackathon.domain.fare.dto;

public record FareQuoteResponse(
        Integer averageFare,
        Integer sameDayThreshold,
        Integer distanceKm,
        String verdict,
        String message
) {
}
