package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜品规格实体，映射 dish_spec 表，保存规格组、选项名称和相对加价。
 * 由菜品、购物车和订单服务通过 DishSpecMapper 使用；历史订单应保存规格名称快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dish_spec")
public class DishSpec {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dishId;
    private String groupName;
    private String name;
    private BigDecimal priceOffset;
    private Integer sortOrder;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
