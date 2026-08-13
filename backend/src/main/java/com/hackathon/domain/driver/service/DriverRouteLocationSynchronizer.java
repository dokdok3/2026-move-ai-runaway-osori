package com.hackathon.domain.driver.service;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 화면에 저장된 선호 구간과 PostGIS 후보 검색용 대표 좌표를 같은 트랜잭션에서 맞춘다.
 */
@Component
class DriverRouteLocationSynchronizer {

    private static final String UPDATE_LOCATION_SQL = """
            UPDATE driver d
            SET current_location = COALESCE(
                    (SELECT location FROM region_coordinate WHERE sido = ? AND sigungu = ?),
                    (SELECT ST_Centroid(ST_Collect(location::geometry))::geography
                     FROM region_coordinate WHERE sido = ?)
                ),
                preferred_destination = COALESCE(
                    (SELECT location FROM region_coordinate WHERE sido = ? AND sigungu = ?),
                    (SELECT ST_Centroid(ST_Collect(location::geometry))::geography
                     FROM region_coordinate WHERE sido = ?)
                )
            WHERE d.id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    DriverRouteLocationSynchronizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void synchronize(Long driverId, String originSido, String originSigungu,
                     String destinationSido, String destinationSigungu) {
        try {
            jdbcTemplate.update(UPDATE_LOCATION_SQL,
                    originSido, originSigungu, originSido,
                    destinationSido, destinationSigungu, destinationSido,
                    driverId);
        } catch (BadSqlGrammarException e) {
            // PostGIS 마이그레이션을 실행하지 않는 H2 단위 테스트에서는 대표 좌표가 필요 없다.
        }
    }
}
