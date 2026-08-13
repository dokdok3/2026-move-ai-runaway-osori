package com.hackathon.global.response;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext,
        int size
) {
}
