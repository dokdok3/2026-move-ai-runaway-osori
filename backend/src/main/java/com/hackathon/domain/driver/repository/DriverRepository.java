package com.hackathon.domain.driver.repository;

import com.hackathon.domain.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}
