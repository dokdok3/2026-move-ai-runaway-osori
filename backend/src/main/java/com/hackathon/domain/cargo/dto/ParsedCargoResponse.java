package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.driver.dto.RegionPoint;

/**
 * 자연어 파싱 결과. 읽지 못한 항목은 null로 내려 프론트가 "고치기"로 채우게 한다.
 * loadingAt/unloadingAt은 ISO-8601 문자열("2026-08-12T14:00:00")로 내려준다.
 */
public record ParsedCargoResponse(
        RegionPoint origin,
        RegionPoint destination,
        String cargoType,
        String cargoDescription,
        Double weightTon,
        String vehicleType,
        Integer desiredFare,
        String loadingAt,
        String unloadingAt
) {
}
