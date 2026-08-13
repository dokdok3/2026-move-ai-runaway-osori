package com.hackathon.domain.cargo.controller;

import com.hackathon.domain.cargo.dto.CargoCreateRequest;
import com.hackathon.domain.cargo.dto.CargoDetailResponse;
import com.hackathon.domain.cargo.dto.CargoUpdateRequest;
import com.hackathon.domain.cargo.dto.ParseRequest;
import com.hackathon.domain.cargo.dto.ParsedCargoResponse;
import com.hackathon.domain.cargo.dto.ShipperCargoResponse;
import com.hackathon.domain.cargo.service.CargoParseService;
import com.hackathon.domain.cargo.service.CargoService;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cargos")
@RequiredArgsConstructor
@Tag(name = "Cargo")
public class CargoController {

    private final CargoService cargoService;
    private final CargoParseService cargoParseService;

    @PostMapping("/parse")
    @Operation(summary = "자연어 문장을 화물 항목으로 파싱 (저장하지 않음)")
    public ApiResponse<ParsedCargoResponse> parse(@Valid @RequestBody ParseRequest request) {
        return ApiResponse.ok(cargoParseService.parse(request));
    }

    @PostMapping
    @Operation(summary = "화물 등록")
    public ApiResponse<Map<String, Object>> create(@LoginUser Long shipperId,
                                                   @Valid @RequestBody CargoCreateRequest request) {
        Long cargoId = cargoService.create(shipperId, request);
        return ApiResponse.ok(Map.of("cargoId", cargoId, "status", "REQUESTED"));
    }

    @GetMapping("/me")
    @Operation(summary = "내가 등록한 화물 목록 조회")
    public ApiResponse<List<ShipperCargoResponse>> getMyCargos(@LoginUser Long shipperId) {
        return ApiResponse.ok(cargoService.findMyCargos(shipperId));
    }

    @GetMapping("/{cargoId}")
    @Operation(summary = "화물 상세 조회")
    public ApiResponse<CargoDetailResponse> detail(@PathVariable Long cargoId) {
        return ApiResponse.ok(cargoService.findDetail(cargoId));
    }

    @PatchMapping("/{cargoId}")
    @Operation(summary = "화물 부분 수정 (보낸 필드만 반영)")
    public ApiResponse<CargoDetailResponse> update(@PathVariable Long cargoId,
                                                    @LoginUser Long shipperId,
                                                    @RequestBody CargoUpdateRequest request) {
        return ApiResponse.ok(cargoService.update(shipperId, cargoId, request));
    }

    @DeleteMapping("/{cargoId}")
    @Operation(summary = "내 화물 삭제 (배차 대기 상태만 가능)")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long cargoId, @LoginUser Long shipperId) {
        cargoService.delete(shipperId, cargoId);
        return ApiResponse.ok(Map.of("cargoId", cargoId, "status", "DELETED"));
    }
}
