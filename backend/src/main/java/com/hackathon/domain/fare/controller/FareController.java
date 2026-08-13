package com.hackathon.domain.fare.controller;

import com.hackathon.domain.cargo.entity.CargoType;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import com.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fares")
@Tag(name = "Fare")
public class FareController {

    @GetMapping("/quote")
    @Operation(summary = "구간 운임 시세 조회 (DB 캐시 우선, miss 시 AI 추정)")
    public ApiResponse<FareQuoteResponse> quote(@RequestParam String originSido,
                                                @RequestParam String destSido,
                                                @RequestParam CargoType cargoType,
                                                @RequestParam BigDecimal weightTon,
                                                @RequestParam(required = false) Integer desiredFare) {
        throw new UnsupportedOperationException("서비스 미구현");
    }
}
