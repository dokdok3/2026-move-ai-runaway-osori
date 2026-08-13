package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.matching.entity.Assignment;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 완료 상태 변경을 짧은 트랜잭션으로 끝내 AI 추천 호출 중 DB 락을 유지하지 않는다. */
@Service
@RequiredArgsConstructor
public class CargoCompletionProcessor {

    private final DriverRepository driverRepository;
    private final CargoRepository cargoRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional
    public CompletionResult complete(Long driverId, Long cargoId) {
        driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_FOUND));
        Assignment assignment = assignmentRepository.findByCargoId(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_ASSIGNED));

        if (!assignment.getDriverId().equals(driverId)) {
            throw new BusinessException(ErrorCode.CARGO_ASSIGNMENT_ACCESS_DENIED);
        }
        if (cargo.getStatus() == CargoStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CARGO_ALREADY_COMPLETED);
        }
        if (cargo.getStatus() != CargoStatus.MATCHED) {
            throw new BusinessException(ErrorCode.CARGO_NOT_COMPLETABLE);
        }

        int updated = cargoRepository.updateStatusIf(cargoId, CargoStatus.MATCHED, CargoStatus.COMPLETED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CARGO_ALREADY_COMPLETED);
        }

        assignment.complete();
        Assignment completedAssignment = assignmentRepository.save(assignment);
        return new CompletionResult(
                completedAssignment.getId(),
                cargoId,
                completedAssignment.getCompletedAt()
        );
    }

    public record CompletionResult(Long assignmentId, Long cargoId, LocalDateTime completedAt) {
    }
}
