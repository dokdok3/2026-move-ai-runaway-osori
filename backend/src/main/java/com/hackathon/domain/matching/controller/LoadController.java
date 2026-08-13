package com.hackathon.domain.matching.controller;

import com.hackathon.domain.matching.dto.AcceptResponse;
import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loads")
@Tag(name = "Load")
public class LoadController {

    @GetMapping
    @Operation(summary = "기사 구간 기준 매칭 점수순 화물 목록")
    public ApiResponse<List<LoadResponse>> getLoads(@LoginUser Long driverId) {
        throw new UnsupportedOperationException("서비스 미구현");
    }

    @PostMapping("/{cargoId}/accept")
    @Operation(summary = "화물 수락")
    public ApiResponse<AcceptResponse> accept(@LoginUser Long driverId,
                                              @PathVariable Long cargoId) {
        throw new UnsupportedOperationException("서비스 미구현");
    }
}
