package com.hackathon.domain.matching.repository;

import com.hackathon.domain.matching.entity.DriverHiddenCargo;
import com.hackathon.domain.matching.entity.DriverHiddenCargoId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverHiddenCargoRepository extends JpaRepository<DriverHiddenCargo, DriverHiddenCargoId> {

    List<DriverHiddenCargo> findByDriverIdOrderByHiddenAtDesc(Long driverId);
}
