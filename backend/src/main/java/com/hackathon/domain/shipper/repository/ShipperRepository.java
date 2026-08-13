package com.hackathon.domain.shipper.repository;

import com.hackathon.domain.shipper.entity.Shipper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipperRepository extends JpaRepository<Shipper, Long> {
}
