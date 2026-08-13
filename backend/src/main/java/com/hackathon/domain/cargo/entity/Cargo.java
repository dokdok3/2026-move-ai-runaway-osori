package com.hackathon.domain.cargo.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cargo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long shipperId;

    private String originSido;
    private String originSigungu;
    private String destSido;
    private String destSigungu;

    @Enumerated(EnumType.STRING)
    private CargoType cargoType;

    /** mock-data.md의 cargoDescription. 화면에 노출되는 화물 한 줄 설명 (예: "냉장식품"). */
    private String cargoDescription;

    private BigDecimal weightTon;
    private String vehicleType;
    private String bodyType;

    private Integer desiredFare;
    private LocalDateTime loadingAt;
    private LocalDateTime unloadingAt;
    private Integer distanceKm;

    @Enumerated(EnumType.STRING)
    private CargoStatus status;

    /** 화주가 "AI로 변환하기" 화면(자연어 파싱)을 거쳐 등록했는지. 직접 입력 폼은 false. */
    private Boolean aiParsed;

    public static Cargo create(Long shipperId,
                               String originSido, String originSigungu,
                               String destSido, String destSigungu,
                               CargoType cargoType, String cargoDescription, BigDecimal weightTon,
                               String vehicleType, String bodyType,
                               Integer desiredFare,
                               LocalDateTime loadingAt, LocalDateTime unloadingAt,
                               Integer distanceKm) {
        Cargo cargo = new Cargo();
        cargo.shipperId = shipperId;
        cargo.originSido = originSido;
        cargo.originSigungu = originSigungu;
        cargo.destSido = destSido;
        cargo.destSigungu = destSigungu;
        cargo.cargoType = cargoType;
        cargo.cargoDescription = cargoDescription;
        cargo.weightTon = weightTon;
        cargo.vehicleType = vehicleType;
        cargo.bodyType = bodyType;
        cargo.desiredFare = desiredFare;
        cargo.loadingAt = loadingAt;
        cargo.unloadingAt = unloadingAt;
        cargo.distanceKm = distanceKm;
        cargo.status = CargoStatus.REQUESTED;
        cargo.aiParsed = false;
        return cargo;
    }

    public void markAiParsed(boolean aiParsed) {
        this.aiParsed = aiParsed;
    }

    public void changeFare(Integer desiredFare) {
        this.desiredFare = desiredFare;
    }

    public void changeSchedule(LocalDateTime loadingAt, LocalDateTime unloadingAt) {
        if (loadingAt != null) {
            this.loadingAt = loadingAt;
        }
        if (unloadingAt != null) {
            this.unloadingAt = unloadingAt;
        }
    }

    public void changeRoute(String originSido, String originSigungu,
                            String destSido, String destSigungu, Integer distanceKm) {
        this.originSido = originSido;
        this.originSigungu = originSigungu;
        this.destSido = destSido;
        this.destSigungu = destSigungu;
        if (distanceKm != null) {
            this.distanceKm = distanceKm;
        }
    }

    public void changeCargoSpec(CargoType cargoType, String cargoDescription, BigDecimal weightTon,
                                String vehicleType, String bodyType) {
        if (cargoType != null) {
            this.cargoType = cargoType;
        }
        if (cargoDescription != null) {
            this.cargoDescription = cargoDescription;
        }
        if (weightTon != null) {
            this.weightTon = weightTon;
        }
        if (vehicleType != null) {
            this.vehicleType = vehicleType;
        }
        if (bodyType != null) {
            this.bodyType = bodyType;
        }
    }

    public boolean isModifiable() {
        return status == CargoStatus.REQUESTED;
    }
}
