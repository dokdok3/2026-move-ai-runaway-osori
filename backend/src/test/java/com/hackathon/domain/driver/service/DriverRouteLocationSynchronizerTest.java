package com.hackathon.domain.driver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class DriverRouteLocationSynchronizerTest {

    @Test
    @DisplayName("시군구 전체 선호는 시도 중심 좌표로 동기화한다")
    void synchronizesWholeSidoToCentroid() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DriverRouteLocationSynchronizer synchronizer =
                new DriverRouteLocationSynchronizer(jdbcTemplate);

        synchronizer.synchronize(1L, "경기도", null, "부산광역시", null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture(), aryEq(new Object[]{
                "경기도", null, "경기도",
                "부산광역시", null, "부산광역시",
                1L
        }));
        assertThat(sql.getValue())
                .contains("ST_Centroid(ST_Collect(location::geometry))::geography");
    }
}
