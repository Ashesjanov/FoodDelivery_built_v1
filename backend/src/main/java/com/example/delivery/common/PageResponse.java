package com.example.delivery.common;

import java.util.List;

/**
 * 列表接口使用的分页响应，页码从 1 开始，并与 MyBatis-Plus 的分页对象解耦。
 * service 将查询结果转换为该 record，controller 直接返回给前端。
 */
public record PageResponse<T>(List<T> records, long total, int page, int size, long pages) {

    public static <T> PageResponse<T> of(List<T> records, long total, int page, int size) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(List.copyOf(records), total, page, size, pages);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), 0L, page, size, 0L);
    }
}
