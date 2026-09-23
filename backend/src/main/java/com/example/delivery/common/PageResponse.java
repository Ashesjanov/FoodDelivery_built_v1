package com.example.delivery.common;

import java.util.List;

/** One-based pagination envelope independent from the persistence layer. */
public record PageResponse<T>(List<T> records, long total, int page, int size, long pages) {

    public static <T> PageResponse<T> of(List<T> records, long total, int page, int size) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(List.copyOf(records), total, page, size, pages);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), 0L, page, size, 0L);
    }
}
