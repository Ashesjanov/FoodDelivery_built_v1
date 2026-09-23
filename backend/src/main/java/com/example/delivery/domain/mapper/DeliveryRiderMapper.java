package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.DeliveryRider;

/**
 * delivery_rider 表的 MyBatis-Plus mapper，为骑手档案和派单筛选提供数据访问。
 * 当前位置与活跃单数会频繁变化，更新时应通过 service 的事务规则避免丢失并发写入。
 */
public interface DeliveryRiderMapper extends BaseMapper<DeliveryRider> {
}
