package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.RiderStatus;
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
@TableName("delivery_rider")
public class DeliveryRider {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String vehicleType;
    private RiderStatus status;
    private BigDecimal currentLongitude;
    private BigDecimal currentLatitude;
    private BigDecimal rating;
    private Integer completedCount;
    private Integer activeOrderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
