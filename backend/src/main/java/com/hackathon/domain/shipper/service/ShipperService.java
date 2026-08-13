package com.hackathon.domain.shipper.service;

import com.hackathon.domain.shipper.dto.ShipperProfileRequest;
import com.hackathon.domain.shipper.dto.ShipperProfileResponse;
import com.hackathon.domain.shipper.entity.Shipper;
import com.hackathon.domain.shipper.repository.ShipperRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipperService {

    private final ShipperRepository shipperRepository;

    @Transactional(readOnly = true)
    public ShipperProfileResponse findMe(Long shipperId) {
        return ShipperProfileResponse.from(getShipper(shipperId));
    }

    @Transactional
    public ShipperProfileResponse updateMe(Long shipperId, ShipperProfileRequest request) {
        Shipper shipper = getShipper(shipperId);
        shipper.update(request.companyName(), request.contactName(), request.phoneNumber(),
                request.businessNumber(), request.address());
        return ShipperProfileResponse.from(shipper);
    }

    private Shipper getShipper(Long shipperId) {
        return shipperRepository.findById(shipperId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPER_NOT_FOUND));
    }
}
