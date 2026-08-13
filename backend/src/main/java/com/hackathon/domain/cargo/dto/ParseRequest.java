package com.hackathon.domain.cargo.dto;

import jakarta.validation.constraints.NotBlank;

public record ParseRequest(
        @NotBlank(message = "파싱할 문장을 입력해주세요.") String rawText
) {
}
