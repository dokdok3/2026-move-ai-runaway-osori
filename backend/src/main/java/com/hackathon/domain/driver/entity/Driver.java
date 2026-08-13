package com.hackathon.domain.driver.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Driver extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phoneNumber;
    private String plateNumber;

    private String vehicleType;      // 카고, 탑차
    private BigDecimal capacityTon;  // 최대 적재량
    private String bodyType;         // 윙바디, 냉장 등

    /** mock-data.md의 vehicleCargoTypes 컬럼 형식(pipe-separated CargoType). 예: "REFRIGERATED|GENERAL" */
    private String vehicleCargoTypes;

    private BigDecimal rating;
    private Integer totalTrips;
    private Integer completionRate;
    private Integer minAcceptFare;

    private String contactableFrom;  // "06:00"
    private String contactableTo;    // "20:00"
    private String recentTripSummary;

    public static Driver create(String name, String phoneNumber, String plateNumber,
                                String vehicleType, BigDecimal capacityTon, String bodyType,
                                String vehicleCargoTypes,
                                BigDecimal rating, Integer totalTrips, Integer completionRate,
                                Integer minAcceptFare, String contactableFrom, String contactableTo,
                                String recentTripSummary) {
        Driver driver = new Driver();
        driver.name = name;
        driver.phoneNumber = phoneNumber;
        driver.plateNumber = plateNumber;
        driver.vehicleType = vehicleType;
        driver.capacityTon = capacityTon;
        driver.bodyType = bodyType;
        driver.vehicleCargoTypes = vehicleCargoTypes;
        driver.rating = rating;
        driver.totalTrips = totalTrips;
        driver.completionRate = completionRate;
        driver.minAcceptFare = minAcceptFare;
        driver.contactableFrom = contactableFrom;
        driver.contactableTo = contactableTo;
        driver.recentTripSummary = recentTripSummary;
        return driver;
    }
}
