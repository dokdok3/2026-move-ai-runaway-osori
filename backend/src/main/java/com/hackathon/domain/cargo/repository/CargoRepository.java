package com.hackathon.domain.cargo.repository;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.entity.CargoType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    List<Cargo> findByStatus(CargoStatus status);

    List<Cargo> findByShipperIdOrderByCreatedAtDesc(Long shipperId);

    List<Cargo> findByOriginSidoAndDestSidoAndCargoType(String originSido, String destSido,
                                                         CargoType cargoType);

    /**
     * status가 from일 때만 to로 바꾼다. 영향 행 수가 0이면 다른 기사가 먼저 가져간 것.
     * 별도의 락 없이 동시 수락을 막는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Cargo c SET c.status = :to WHERE c.id = :id AND c.status = :from")
    int updateStatusIf(@Param("id") Long id,
                       @Param("from") CargoStatus from,
                       @Param("to") CargoStatus to);
}
