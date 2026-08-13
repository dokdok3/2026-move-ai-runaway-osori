package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.entity.LoadType;
import com.hackathon.domain.matching.service.LoadCursorPaginator;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.response.CursorPageResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoadCursorPaginatorTest {

    private final LoadCursorPaginator paginator = new LoadCursorPaginator();

    @Test
    @DisplayName("화물 목록을 커서 기준 10건씩 중복 없이 반환한다")
    void paginatesTenLoadsWithoutDuplicates() {
        List<LoadResponse> loads = LongStream.rangeClosed(1, 21)
                .mapToObj(this::load)
                .toList();

        CursorPageResponse<LoadResponse> first = paginator.paginate(loads, 1L, LoadType.ALL, null);
        CursorPageResponse<LoadResponse> second = paginator.paginate(
                loads, 1L, LoadType.ALL, first.nextCursor());
        CursorPageResponse<LoadResponse> third = paginator.paginate(
                loads, 1L, LoadType.ALL, second.nextCursor());

        assertThat(first.content()).hasSize(10).extracting(LoadResponse::cargoId)
                .containsExactlyElementsOf(LongStream.rangeClosed(1, 10).boxed().toList());
        assertThat(second.content()).hasSize(10).extracting(LoadResponse::cargoId)
                .containsExactlyElementsOf(LongStream.rangeClosed(11, 20).boxed().toList());
        assertThat(third.content()).hasSize(1).extracting(LoadResponse::cargoId).containsExactly(21L);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isTrue();
        assertThat(third.hasNext()).isFalse();
        assertThat(third.nextCursor()).isNull();
        assertThat(first.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("다른 기사나 필터의 커서는 사용할 수 없다")
    void rejectsCursorFromDifferentContext() {
        List<LoadResponse> loads = LongStream.rangeClosed(1, 11).mapToObj(this::load).toList();
        String cursor = paginator.paginate(loads, 1L, LoadType.ALL, null).nextCursor();

        assertThatThrownBy(() -> paginator.paginate(loads, 2L, LoadType.ALL, cursor))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> paginator.paginate(loads, 1L, LoadType.HIDDEN, cursor))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("형식이 잘못된 커서는 사용할 수 없다")
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> paginator.paginate(List.of(load(1L)), 1L, LoadType.ALL, "invalid"))
                .isInstanceOf(BusinessException.class);
    }

    private LoadResponse load(long cargoId) {
        return new LoadResponse(cargoId, "서울 송파구", "부산 강서구",
                LocalDateTime.of(2026, 8, 14, 8, 0), LocalDateTime.of(2026, 8, 14, 15, 0),
                null, null, new BigDecimal("5.0"), null, "GENERAL", 600_000, 80,
                "FAIR", 600_000, null, null, null, null, null, "RULE_BASE", List.of(),
                "REQUESTED", null);
    }
}
