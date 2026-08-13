package com.hackathon.domain.shipper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipperProfileRequest(
        @NotBlank @Size(max = 100) String companyName,
        @NotBlank @Size(max = 50) String contactName,
        @NotBlank @Size(max = 20) String phoneNumber,
        @Size(max = 20) String businessNumber,
        @Size(max = 200) String address
) {
}
