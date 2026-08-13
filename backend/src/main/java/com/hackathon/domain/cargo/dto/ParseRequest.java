package com.hackathon.domain.cargo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ParseRequest(
        @JsonAlias("rawText")
        @NotBlank(message = "파싱할 문장을 입력해주세요.") String requestText,
        LocalDate referenceDate
) {
}
