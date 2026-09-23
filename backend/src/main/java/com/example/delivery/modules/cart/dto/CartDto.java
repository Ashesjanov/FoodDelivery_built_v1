package com.example.delivery.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
