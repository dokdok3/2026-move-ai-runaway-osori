package com.hackathon.domain.region.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegionCoordinateRepository {

    private static final String FIND_ALL_SQL = """
            SELECT sido, sigungu
            FROM region_coordinate
            ORDER BY sido, sigungu
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<RegionCoordinate> findAll() {
        return jdbcTemplate.query(FIND_ALL_SQL, (resultSet, rowNum) -> new RegionCoordinate(
                resultSet.getString("sido"),
                resultSet.getString("sigungu")
        ));
    }

    public record RegionCoordinate(String sido, String sigungu) {
    }
}
