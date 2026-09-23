package com.example.delivery.modules.dish.dto;

import com.example.delivery.domain.entity.Dish;
import com.example.delivery.domain.enums.DishStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜品和规格的请求、响应 DTO 集合，供商家目录管理和顾客目录查询使用。
 * 名称、价格、库存等由 Bean Validation 在 REST 边界校验，
 * 响应映射只暴露定价、库存和生命周期状态等安全字段。
 */
public final class DishDto {
    private DishDto() {
    }

    public record SaveRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            @Size(max = 255) String imageUrl,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal originalPrice,
            @NotNull @Min(0) Integer stock,
            Integer sortOrder
    ) {
    }

    public record StatusRequest(@NotNull DishStatus status) {
    }

    public record SpecRequest(
            @Size(max = 50) String groupName,
            @NotBlank @Size(max = 50) String name,
            BigDecimal priceOffset,
            Integer sortOrder,
            Boolean isDefault
    ) {
    }

    public record SpecView(Long id, Long dishId, String groupName, String name, BigDecimal priceOffset,
                           Integer sortOrder, Boolean isDefault) {
    }

    public record DishView(
            Long id, Long merchantId, Long categoryId, String name, String description, String imageUrl,
            BigDecimal price, BigDecimal originalPrice, DishStatus status, Integer stock, Integer sales,
            Integer sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        public static DishView from(Dish dish) {
            return new DishView(dish.getId(), dish.getMerchantId(), dish.getCategoryId(), dish.getName(), dish.getDescription(),
                    dish.getImageUrl(), dish.getPrice(), dish.getOriginalPrice(), dish.getStatus(), dish.getStock(), dish.getSales(),
                    dish.getSortOrder(), dish.getCreatedAt(), dish.getUpdatedAt());
        }
    }

    public record DishDetailView(DishView dish, List<SpecView> specs) {
    }
}
