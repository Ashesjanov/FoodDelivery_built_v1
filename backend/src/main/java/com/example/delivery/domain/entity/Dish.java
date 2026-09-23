package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.DishStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dish")
public class Dish {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long categoryId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private DishStatus status;
    private Integer stock;
    private Integer sales;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
