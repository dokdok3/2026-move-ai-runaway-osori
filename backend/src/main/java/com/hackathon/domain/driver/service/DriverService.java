package com.hackathon.domain.driver.service;

import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.dto.RoutePreferenceRequest;
import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public DriverResponse findMe(Long driverId) {
        Driver driver = getDriver(driverId);
        return DriverResponse.from(driver, preferenceRepository.findByDriverId(driverId));
    }

    @Transactional
    public DriverResponse updateRoutePreferences(Long driverId, RoutePreferenceRequest request) {
        Driver driver = getDriver(driverId);

        preferenceRepository.deleteByDriverId(driverId);
        preferenceRepository.flush();

        List<DriverRoutePreference> saved = new ArrayList<>();
        request.origins().forEach(p ->
                saved.add(DriverRoutePreference.of(driverId, Direction.ORIGIN, p.sido(), p.sigungu())));
        request.destinations().forEach(p ->
                saved.add(DriverRoutePreference.of(driverId, Direction.DESTINATION, p.sido(), p.sigungu())));

        return DriverResponse.from(driver, preferenceRepository.saveAll(saved));
    }

    private Driver getDriver(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
    }
}
