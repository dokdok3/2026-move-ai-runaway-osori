package com.hackathon.domain.region.controller;

import com.hackathon.domain.region.dto.RegionResponse;
import com.hackathon.domain.region.service.RegionService;
import com.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Tag(name = "Region")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    @Operation(summary = "시도·시군구 지역 목록 조회")
    public ApiResponse<List<RegionResponse>> getRegions() {
        return ApiResponse.ok(regionService.findAll());
    }
}
