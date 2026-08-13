package com.hackathon.domain.driver.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record DriverProfileRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 20) String plateNumber,
        @NotBlank @Size(max = 20) String vehicleType,
        @NotNull @DecimalMin("0.1") @Digits(integer = 5, fraction = 1) BigDecimal capacityTon,
        @NotBlank @Size(max = 20) String bodyType,
        @NotEmpty @Size(max = 5) List<@Valid @Pattern(
                regexp = "REFRIGERATED|GENERAL|FROZEN|CONSTRUCTION|HAZARDOUS",
                message = "지원하지 않는 화물 종류입니다.") String> vehicleCargoTypes,
        @NotNull @PositiveOrZero Integer minAcceptFare,
        @NotBlank @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "HH:mm 형식이어야 합니다.")
        String contactableFrom,
        @NotBlank @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "HH:mm 형식이어야 합니다.")
        String contactableTo
) {
}
