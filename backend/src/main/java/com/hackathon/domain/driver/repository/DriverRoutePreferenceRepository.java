package com.hackathon.domain.driver.repository;

import com.hackathon.domain.driver.entity.DriverRoutePreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRoutePreferenceRepository extends JpaRepository<DriverRoutePreference, Long> {

    List<DriverRoutePreference> findByDriverId(Long driverId);

    void deleteByDriverId(Long driverId);
}
