package com.hackathon.domain.matching.controller;

import com.hackathon.domain.matching.dto.AcceptResponse;
import com.hackathon.domain.matching.dto.CompletedLoadResponse;
import com.hackathon.domain.matching.dto.CompleteResponse;
import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.entity.LoadType;
import com.hackathon.domain.matching.service.LoadCursorPaginator;
import com.hackathon.domain.matching.service.LoadService;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import com.hackathon.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loads")
@RequiredArgsConstructor
@Tag(name = "Load")
public class LoadController {

    private final LoadService loadService;
    private final LoadCursorPaginator loadCursorPaginator;

    @GetMapping
    @Operation(summary = "기사 화물 목록 조회 (10건 커서 페이지네이션)")
    public ApiResponse<CursorPageResponse<LoadResponse>> getLoads(
            @LoginUser Long driverId,
            @RequestParam(name = "filter", defaultValue = "ALL") LoadType filter,
            @RequestParam(required = false) String preferenceText,
            @RequestParam(defaultValue = "false") boolean refresh,
            @RequestParam(required = false) String cursor) {
        List<LoadResponse> loads = loadService.findLoads(driverId, filter, preferenceText, refresh);
        return ApiResponse.ok(loadCursorPaginator.paginate(loads, driverId, filter, cursor));
    }

    @GetMapping("/completed")
    @Operation(summary = "기사가 완료한 화물 목록")
    public ApiResponse<List<CompletedLoadResponse>> getCompletedLoads(@LoginUser Long driverId) {
        return ApiResponse.ok(loadService.findCompletedLoads(driverId));
    }

    @PostMapping("/{cargoId}/accept")
    @Operation(summary = "화물 수락")
    public ApiResponse<AcceptResponse> accept(@LoginUser Long driverId,
                                              @PathVariable Long cargoId) {
        return ApiResponse.ok(loadService.accept(driverId, cargoId));
    }

    @PostMapping("/{cargoId}/complete")
    @Operation(summary = "수락한 화물 운송 완료")
    public ApiResponse<CompleteResponse> complete(@LoginUser Long driverId,
                                                   @PathVariable Long cargoId) {
        return ApiResponse.ok(loadService.complete(driverId, cargoId));
    }

    @PostMapping("/{cargoId}/hide")
    @Operation(summary = "화물 목록에서 숨기기")
    public ApiResponse<Void> hide(@LoginUser Long driverId,
                                  @PathVariable Long cargoId) {
        loadService.hide(driverId, cargoId);
        return ApiResponse.ok(null);
    }
}
