package com.hackathon.domain.matching.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
public class DriverHiddenCargoId implements Serializable {

    private Long driverId;
    private Long cargoId;

    public DriverHiddenCargoId(Long driverId, Long cargoId) {
        this.driverId = Objects.requireNonNull(driverId);
        this.cargoId = Objects.requireNonNull(cargoId);
    }
}
