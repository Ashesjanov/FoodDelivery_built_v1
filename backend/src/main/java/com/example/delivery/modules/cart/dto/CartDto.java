package com.example.delivery.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车请求和响应 DTO 集合，覆盖加购、更新、全选和查询。
 * 数量、备注等字段由 Bean Validation 在 REST 边界校验，
 * 响应记录包含计算后的成交单价和条目小计。
 */
public final class CartDto {
    private CartDto() {
    }

    public record AddItemRequest(@NotNull Long dishId, Long dishSpecId, @NotNull @Min(1) Integer quantity,
                                 @Size(max = 200) String note) {
    }

    public record UpdateItemRequest(@Min(1) Integer quantity, Boolean selected, @Size(max = 200) String note) {
    }

    public record SelectAllRequest(@NotNull Boolean selected) {
    }

    public record ItemView(Long id, Long merchantId, Long dishId, Long dishSpecId, String dishName, String specName,
                           String imageUrl, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal, Boolean selected,
                           String note, LocalDateTime updatedAt) {
    }

    public record CartView(List<ItemView> items, BigDecimal totalAmount, int itemCount) {
    }
}
