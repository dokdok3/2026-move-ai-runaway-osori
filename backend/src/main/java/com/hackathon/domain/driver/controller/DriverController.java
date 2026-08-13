package com.hackathon.domain.driver.controller;

import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.dto.RoutePreferenceRequest;
import com.hackathon.domain.driver.service.DriverService;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver")
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/me")
    @Operation(summary = "내 기사 프로필 조회")
    public ApiResponse<DriverResponse> getMe(@LoginUser Long driverId) {
        return ApiResponse.ok(driverService.findMe(driverId));
    }

    @PutMapping("/me/route-preferences")
    @Operation(summary = "다니는 구간 전체 교체 저장")
    public ApiResponse<DriverResponse> updateRoutePreferences(
            @LoginUser Long driverId,
            @Valid @RequestBody RoutePreferenceRequest request) {
        return ApiResponse.ok(driverService.updateRoutePreferences(driverId, request));
    }
}
