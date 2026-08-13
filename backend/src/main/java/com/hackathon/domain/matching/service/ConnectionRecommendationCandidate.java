package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;

public record ConnectionRecommendationCandidate(
        Cargo cargo,
        double emptyDistanceKm,
        long waitMinutes,
        int connectionScore
) {
}
