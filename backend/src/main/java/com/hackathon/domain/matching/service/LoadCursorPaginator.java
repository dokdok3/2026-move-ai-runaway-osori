package com.hackathon.domain.matching.service;

import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.entity.LoadType;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import com.hackathon.global.response.CursorPageResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LoadCursorPaginator {

    private static final int PAGE_SIZE = 10;
    private static final String CURSOR_VERSION = "v1";

    public CursorPageResponse<LoadResponse> paginate(
            List<LoadResponse> loads, Long driverId, LoadType filter, String cursor) {
        int startIndex = startIndex(loads, driverId, filter, cursor);
        int endIndex = Math.min(startIndex + PAGE_SIZE, loads.size());
        List<LoadResponse> content = List.copyOf(loads.subList(startIndex, endIndex));
        boolean hasNext = endIndex < loads.size();
        String nextCursor = hasNext
                ? encode(driverId, filter, content.getLast().cargoId())
                : null;
        return new CursorPageResponse<>(content, nextCursor, hasNext, PAGE_SIZE);
    }

    private int startIndex(List<LoadResponse> loads, Long driverId, LoadType filter, String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }

        CursorKey cursorKey = decode(cursor);
        if (!cursorKey.driverId().equals(driverId) || cursorKey.filter() != filter) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        for (int index = 0; index < loads.size(); index++) {
            if (loads.get(index).cargoId().equals(cursorKey.cargoId())) {
                return index + 1;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_CURSOR);
    }

    private String encode(Long driverId, LoadType filter, Long cargoId) {
        String value = "%s:%d:%s:%d".formatted(CURSOR_VERSION, driverId, filter.name(), cargoId);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private CursorKey decode(String cursor) {
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split(":", -1);
            if (parts.length != 4 || !CURSOR_VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("지원하지 않는 커서 형식입니다.");
            }
            return new CursorKey(Long.parseLong(parts[1]), LoadType.valueOf(parts[2]),
                    Long.parseLong(parts[3]));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    private record CursorKey(Long driverId, LoadType filter, Long cargoId) {
    }
}
