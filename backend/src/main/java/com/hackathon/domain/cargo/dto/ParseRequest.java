package com.hackathon.domain.cargo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ParseRequest(
        @JsonAlias("rawText")
        @NotBlank(message = "화물 요청 내용은 필수입니다.") String requestText,
        LocalDate referenceDate
) {
}
