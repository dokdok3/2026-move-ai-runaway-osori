package com.hackathon.domain.fare.dto;

import com.hackathon.domain.fare.entity.FareQuote;

public record FareQuoteResponse(
        Integer averageFare,
        Integer sameDayThreshold,
        Integer distanceKm,
        String verdict,
        String message
) {
    public static FareQuoteResponse of(FareQuote quote, Integer desiredFare) {
        if (desiredFare == null) {
            return new FareQuoteResponse(quote.getAverageFare(), quote.getSameDayThreshold(),
                    quote.getDistanceKm(), null, null);
        }

        boolean fair = desiredFare >= quote.getSameDayThreshold();
        String verdict = fair ? "FAIR" : "SLOW";
        String message = fair ? null : slowMessage(quote.getAverageFare(), desiredFare);
        return new FareQuoteResponse(quote.getAverageFare(), quote.getSameDayThreshold(),
                quote.getDistanceKm(), verdict, message);
    }

    private static String slowMessage(Integer averageFare, Integer desiredFare) {
        int belowPercent = (int) Math.round((averageFare - desiredFare) * 100.0 / averageFare);
        return "입력하신 운임이 이 구간 평균(%,d원)보다 %d%% 낮아요. 확인해보세요".formatted(averageFare, belowPercent);
    }
}
