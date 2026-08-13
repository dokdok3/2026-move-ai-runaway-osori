package com.hackathon.domain.matching.repository;

import com.hackathon.domain.matching.entity.Assignment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByCargoId(Long cargoId);
}
