package com.hackathon.domain.matching.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@IdClass(DriverHiddenCargoId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverHiddenCargo {

    @Id
    private Long driverId;

    @Id
    private Long cargoId;

    private LocalDateTime hiddenAt;

    public static DriverHiddenCargo of(Long driverId, Long cargoId) {
        DriverHiddenCargo hidden = new DriverHiddenCargo();
        hidden.driverId = driverId;
        hidden.cargoId = cargoId;
        hidden.hiddenAt = LocalDateTime.now();
        return hidden;
    }
}
