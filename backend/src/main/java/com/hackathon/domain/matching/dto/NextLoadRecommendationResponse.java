package com.hackathon.domain.matching.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record NextLoadRecommendationResponse(
        Long cargoId,
        String origin,
        String destination,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        String cargoType,
        BigDecimal weightTon,
        Integer fare,
        Double emptyDistanceKm,
        Long waitMinutes,
        Integer connectionScore,
        String recommendationSummary,
        List<String> recommendationReasons,
        String explanationMode
) {
    public static NextLoadRecommendationResponse of(
            Cargo cargo,
            double emptyDistanceKm,
            long waitMinutes,
            int connectionScore,
            String recommendationSummary,
            List<String> recommendationReasons,
            String explanationMode
    ) {
        return new NextLoadRecommendationResponse(
                cargo.getId(),
                label(cargo.getOriginSido(), cargo.getOriginSigungu()),
                label(cargo.getDestSido(), cargo.getDestSigungu()),
                cargo.getLoadingAt(),
                cargo.getUnloadingAt(),
                cargo.getCargoType() != null ? cargo.getCargoType().name() : null,
                cargo.getWeightTon(),
                cargo.getDesiredFare(),
                emptyDistanceKm,
                waitMinutes,
                connectionScore,
                recommendationSummary,
                List.copyOf(recommendationReasons),
                explanationMode
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
