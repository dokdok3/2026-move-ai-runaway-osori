package com.hackathon.domain.matching.dto;

import java.time.LocalDateTime;

public record CompleteResponse(
        Long assignmentId,
        Long cargoId,
        String status,
        LocalDateTime completedAt
) {
}
