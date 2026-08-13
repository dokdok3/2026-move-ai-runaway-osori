package com.hackathon.domain.shipper.controller;

import com.hackathon.domain.shipper.dto.ShipperProfileRequest;
import com.hackathon.domain.shipper.dto.ShipperProfileResponse;
import com.hackathon.domain.shipper.service.ShipperService;
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
@RequestMapping("/api/v1/shippers")
@RequiredArgsConstructor
@Tag(name = "Shipper")
public class ShipperController {

    private final ShipperService shipperService;

    @GetMapping("/me")
    @Operation(summary = "내 화주 프로필 조회")
    public ApiResponse<ShipperProfileResponse> getMe(@LoginUser Long shipperId) {
        return ApiResponse.ok(shipperService.findMe(shipperId));
    }

    @PutMapping("/me")
    @Operation(summary = "내 화주 프로필 수정")
    public ApiResponse<ShipperProfileResponse> updateMe(
            @LoginUser Long shipperId,
            @Valid @RequestBody ShipperProfileRequest request) {
        return ApiResponse.ok(shipperService.updateMe(shipperId, request));
    }
}
