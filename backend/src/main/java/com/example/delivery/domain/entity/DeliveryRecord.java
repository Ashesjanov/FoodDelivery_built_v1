package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.DeliveryStatus;
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
@TableName("delivery_record")
public class DeliveryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long riderId;
    private DeliveryStatus status;
    private LocalDateTime acceptedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private String pickupCode;
    private String deliveryNote;
    private BigDecimal distanceKm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
