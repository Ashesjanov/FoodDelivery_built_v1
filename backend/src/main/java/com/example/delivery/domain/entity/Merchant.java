package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.MerchantBusinessStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户门店实体，映射 merchant 表，保存归属、地址、营业状态和履约费用。
 * 由商户、订单和配送服务通过 MerchantMapper 使用；费用与坐标变化需保证精度和取值范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String logoUrl;
    private String contactName;
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private MerchantBusinessStatus businessStatus;
    private String businessHours;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packagingFee;
    private BigDecimal rating;
    private Integer monthlySales;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
