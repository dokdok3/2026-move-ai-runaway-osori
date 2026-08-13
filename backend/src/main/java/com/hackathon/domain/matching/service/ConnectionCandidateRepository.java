package com.hackathon.domain.matching.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 완료 화물의 하차지에서 100km 이내인 연계 배차 후보를 PostGIS로 제한한다. */
@Repository
public class ConnectionCandidateRepository {

    private static final String CANDIDATES_SQL = """
            WITH previous_load AS (
                SELECT destination_location
                FROM cargo
                WHERE id = ?
                  AND destination_location IS NOT NULL
            ), driver_context AS (
                SELECT capacity_ton, vehicle_cargo_types, min_accept_fare
                FROM driver
                WHERE id = ?
            )
            SELECT c.id AS cargo_id,
                   ROUND(ST_Distance(previous_load.destination_location, c.origin_location))::bigint
                       AS empty_distance_m
            FROM cargo c
            CROSS JOIN previous_load
            CROSS JOIN driver_context d
            WHERE c.status = 'REQUESTED'
              AND c.origin_location IS NOT NULL
              AND c.weight_ton <= d.capacity_ton
              AND c.cargo_type = ANY (string_to_array(d.vehicle_cargo_types, '|'))
              AND c.desired_fare >= COALESCE(d.min_accept_fare, 0)
              AND c.loading_at > ?
              AND c.loading_at <= ? + INTERVAL '48 hours'
              AND ST_DWithin(previous_load.destination_location, c.origin_location, 100000)
              AND NOT EXISTS (
                  SELECT 1
                  FROM driver_hidden_cargo hidden
                  WHERE hidden.driver_id = ? AND hidden.cargo_id = c.id
              )
            ORDER BY empty_distance_m ASC, c.loading_at ASC, c.desired_fare DESC, c.id ASC
            LIMIT 20
            """;

    private final JdbcTemplate jdbcTemplate;

    public ConnectionCandidateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PostgisConnectionCandidate> findCandidates(
            Long completedCargoId,
            Long driverId,
            LocalDateTime availableAt
    ) {
        Timestamp timestamp = Timestamp.valueOf(availableAt);
        return jdbcTemplate.query(
                CANDIDATES_SQL,
                (resultSet, rowNum) -> new PostgisConnectionCandidate(
                        resultSet.getLong("cargo_id"),
                        resultSet.getLong("empty_distance_m")
                ),
                completedCargoId,
                driverId,
                timestamp,
                timestamp,
                driverId
        );
    }

    public record PostgisConnectionCandidate(Long cargoId, Long emptyDistanceM) {
    }
}
