package com.hackathon.domain.cargo.controller;

import com.hackathon.domain.cargo.dto.CargoCreateRequest;
import com.hackathon.domain.cargo.dto.CargoDetailResponse;
import com.hackathon.domain.cargo.dto.CargoUpdateRequest;
import com.hackathon.domain.cargo.dto.ParseRequest;
import com.hackathon.domain.cargo.dto.ParsedCargoResponse;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cargos")
@Tag(name = "Cargo")
public class CargoController {

    @PostMapping("/parse")
    @Operation(summary = "자연어 문장을 화물 항목으로 파싱 (저장하지 않음)")
    public ApiResponse<ParsedCargoResponse> parse(@Valid @RequestBody ParseRequest request) {
        throw new UnsupportedOperationException("서비스 미구현");
    }

    @PostMapping
    @Operation(summary = "화물 등록")
    public ApiResponse<Map<String, Object>> create(@LoginUser Long shipperId,
                                                   @Valid @RequestBody CargoCreateRequest request) {
        throw new UnsupportedOperationException("서비스 미구현");
    }

    @GetMapping("/{cargoId}")
    @Operation(summary = "화물 상세 조회")
    public ApiResponse<CargoDetailResponse> detail(@PathVariable Long cargoId) {
        throw new UnsupportedOperationException("서비스 미구현");
    }

    @PatchMapping("/{cargoId}")
    @Operation(summary = "화물 부분 수정 (보낸 필드만 반영)")
    public ApiResponse<CargoDetailResponse> update(@PathVariable Long cargoId,
                                                    @RequestBody CargoUpdateRequest request) {
        throw new UnsupportedOperationException("서비스 미구현");
    }
}
