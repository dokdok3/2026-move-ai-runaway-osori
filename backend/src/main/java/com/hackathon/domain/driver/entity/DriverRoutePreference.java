package com.hackathon.domain.driver.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverRoutePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long driverId;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private String sido;

    /** null이면 해당 시도 전체를 뜻한다. */
    private String sigungu;

    public static DriverRoutePreference of(Long driverId, Direction direction,
                                           String sido, String sigungu) {
        DriverRoutePreference pref = new DriverRoutePreference();
        pref.driverId = driverId;
        pref.direction = direction;
        pref.sido = sido;
        pref.sigungu = sigungu;
        return pref;
    }
}
