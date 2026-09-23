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

/**
 * 购物车条目实体，映射 cart_item 表，保存用户待结算的菜品和规格快照。
 * 由购物车服务通过 CartItemMapper 读写；数量、单价和所属商户必须在结算时再次校验。
 */
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
