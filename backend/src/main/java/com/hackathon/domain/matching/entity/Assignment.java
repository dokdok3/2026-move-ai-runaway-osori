package com.hackathon.domain.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long cargoId;

    private Long driverId;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    public static Assignment of(Long cargoId, Long driverId) {
        Assignment assignment = new Assignment();
        assignment.cargoId = cargoId;
        assignment.driverId = driverId;
        assignment.acceptedAt = LocalDateTime.now();
        return assignment;
    }

    public void complete() {
        this.completedAt = LocalDateTime.now();
    }
}
