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

/**
 * 骑手工作档案实体，映射 delivery_rider 表，保存联系方式、位置、评分和接单能力。
 * 由配送服务通过 DeliveryRiderMapper 查询和更新；位置及活跃单数存在高频并发写入。
 */
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
