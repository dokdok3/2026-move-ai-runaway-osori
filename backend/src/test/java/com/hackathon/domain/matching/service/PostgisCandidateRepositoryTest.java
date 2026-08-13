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
    @DisplayName("시군구 전체 선호는 반경과 무관하게 같은 시도의 후보를 조회한다")
    @SuppressWarnings("unchecked")
    void treatsNullSigunguAsWholeSido() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(1L)))
                .thenReturn(List.of());

        new PostgisCandidateRepository(jdbcTemplate).findTop50(1L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(org.springframework.jdbc.core.RowMapper.class), eq(1L));
        assertThat(sql.getValue())
                .contains("origin_pref.sido = c.origin_sido")
                .contains("origin_pref.sigungu IS NULL OR origin_pref.sigungu = c.origin_sigungu")
                .contains("destination_pref.sido = c.dest_sido")
                .contains("destination_pref.sigungu IS NULL OR destination_pref.sigungu = c.dest_sigungu")
                .contains("origin_all.sigungu IS NULL")
                .contains("destination_all.sigungu IS NULL");
    }
}
