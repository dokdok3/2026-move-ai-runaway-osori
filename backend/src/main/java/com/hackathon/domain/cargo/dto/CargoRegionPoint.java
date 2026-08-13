package com.hackathon.domain.cargo.dto;

import jakarta.validation.constraints.NotBlank;

/** 화물 등록에 필요한 상세 출발·도착지. 기사 활동 지역과 달리 시군구까지 반드시 입력한다. */
public record CargoRegionPoint(
        @NotBlank(message = "시도를 입력해주세요.") String sido,
        @NotBlank(message = "시군구를 입력해주세요.") String sigungu
) {
}
