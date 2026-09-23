package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.CartItemStatus;
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
@TableName("cart_item")
public class CartItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long merchantId;
    private Long dishId;
    private Long dishSpecId;
    private CartItemStatus status;
    private Integer quantity;
    private BigDecimal unitPrice;
    @TableField("selected")
    private Boolean selected;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
