package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户收货地址实体，映射 user_address 表，保存联系人、行政区划和配送坐标。
 * 由用户地址和订单服务通过 UserAddressMapper 使用；默认地址唯一性由服务层维护，下单保存地址快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_address")
public class UserAddress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String contactName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String label;
    @TableField("is_default")
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
