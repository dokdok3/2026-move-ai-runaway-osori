package com.hackathon.domain.driver.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoutePreferenceRequest(
        @NotEmpty(message = "출발 구간을 1개 이상 선택해주세요.")
        List<RegionPoint> origins,

        @NotEmpty(message = "도착 구간을 1개 이상 선택해주세요.")
        List<RegionPoint> destinations
) {
}
