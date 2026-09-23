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

/**
 * 订单配送执行记录实体，映射 delivery_record 表，保存骑手、履约节点和取货凭证。
 * 由配送服务通过 DeliveryRecordMapper 更新，状态与时间戳需与订单进度保持一致。
 */
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
