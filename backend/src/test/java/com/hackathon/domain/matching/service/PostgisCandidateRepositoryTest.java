package com.hackathon.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgisCandidateRepositoryTest {

    @Test
    @DisplayName("전체 선택과 특정 구 선택을 방향별로 독립 적용한다")
    @SuppressWarnings("unchecked")
    void treatsNullSigunguAsWholeSido() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(1L)))
                .thenReturn(List.of());

        new PostgisCandidateRepository(jdbcTemplate).findTop50(1L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(org.springframework.jdbc.core.RowMapper.class), eq(1L));
        assertThat(sql.getValue())
                .contains("origin_all.sigungu IS NULL")
                .contains("OR ST_DWithin(c.origin_location, d.current_location, d.pickup_radius_m)")
                .contains("destination_all.sigungu IS NULL")
                .contains("OR ST_DWithin(c.destination_location, d.preferred_destination, d.destination_radius_m)")
                .contains("ST_Distance(c.origin_location, d.current_location)")
                .contains("ST_Distance(c.destination_location, d.preferred_destination)")
                .doesNotContain("origin_pref.sigungu IS NULL OR origin_pref.sigungu = c.origin_sigungu")
                .doesNotContain("destination_pref.sigungu IS NULL OR destination_pref.sigungu = c.dest_sigungu");
    }
}
