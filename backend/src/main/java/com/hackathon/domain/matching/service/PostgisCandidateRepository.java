package com.hackathon.domain.matching.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostGIS가 필수조건·반경 필터와 기본 점수로 AI 입력 후보를 최대 50건으로 제한한다. */
@Repository
public class PostgisCandidateRepository {

    private static final String CANDIDATES_SQL = """
            WITH driver_context AS (
                SELECT id, current_location, preferred_destination, vehicle_cargo_types,
                       capacity_ton, min_accept_fare, available_from, available_until,
                       pickup_radius_m, destination_radius_m
                FROM driver
                WHERE id = ?
                  AND current_location IS NOT NULL
                  AND preferred_destination IS NOT NULL
            ), eligible AS (
                SELECT c.id, c.desired_fare, c.loading_at,
                       d.min_accept_fare, d.available_from, d.pickup_radius_m, d.destination_radius_m,
                       ST_Distance(d.current_location, c.origin_location) AS pickup_distance_m,
                       ST_Distance(d.preferred_destination, c.destination_location) AS destination_gap_m
                FROM cargo c
                CROSS JOIN driver_context d
                WHERE c.status = 'REQUESTED'
                  AND c.origin_location IS NOT NULL
                  AND c.destination_location IS NOT NULL
                  AND c.cargo_type = ANY (string_to_array(d.vehicle_cargo_types, '|'))
                  AND c.weight_ton <= d.capacity_ton
                  AND c.desired_fare >= d.min_accept_fare
                  AND c.loading_at BETWEEN d.available_from AND d.available_until
                  AND ST_DWithin(c.origin_location, d.current_location, d.pickup_radius_m)
                  AND ST_DWithin(c.destination_location, d.preferred_destination, d.destination_radius_m)
                  AND NOT EXISTS (
                      SELECT 1 FROM driver_hidden_cargo h
                      WHERE h.driver_id = d.id AND h.cargo_id = c.id
                  )
            ), scored AS (
                SELECT *,
                       GREATEST(0.0, 35.0 * (1.0 - pickup_distance_m / pickup_radius_m)) AS pickup_score,
                       GREATEST(0.0, 25.0 * (1.0 - destination_gap_m / destination_radius_m)) AS destination_score,
                       LEAST(20.0, 10.0 + 40.0 * (desired_fare - min_accept_fare)
                           / GREATEST(min_accept_fare, 1)) AS fare_score,
                       10.0 AS cargo_score,
                       GREATEST(0.0, 10.0 - EXTRACT(EPOCH FROM (loading_at - available_from)) / 3600.0) AS time_score
                FROM eligible
            )
            SELECT id,
                   ROUND(pickup_distance_m)::bigint AS pickup_distance_m,
                   ROUND(destination_gap_m)::bigint AS destination_gap_m,
                   ROUND((pickup_score + destination_score + fare_score + cargo_score + time_score)::numeric, 2)::double precision AS base_score
            FROM scored
            ORDER BY base_score DESC, desired_fare DESC, id ASC
            LIMIT 50
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgisCandidateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PostgisCandidate> findTop50(Long driverId) {
        return jdbcTemplate.query(CANDIDATES_SQL, (resultSet, rowNum) -> new PostgisCandidate(
                resultSet.getLong("id"),
                resultSet.getLong("pickup_distance_m"),
                resultSet.getLong("destination_gap_m"),
                resultSet.getDouble("base_score")
        ), driverId);
    }

    public record PostgisCandidate(Long cargoId, Long pickupDistanceM,
                                   Long destinationGapM, Double baseScore) {
    }
}
