package com.hackathon.domain.shipper.dto;

import com.hackathon.domain.shipper.entity.Shipper;

public record ShipperProfileResponse(
        Long shipperId,
        String companyName,
        String contactName,
        String phoneNumber,
        String businessNumber,
        String address
) {
    public static ShipperProfileResponse from(Shipper shipper) {
        return new ShipperProfileResponse(shipper.getId(), shipper.getCompanyName(), shipper.getContactName(),
                shipper.getPhoneNumber(), shipper.getBusinessNumber(), shipper.getAddress());
    }
}
