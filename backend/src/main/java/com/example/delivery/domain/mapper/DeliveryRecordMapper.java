package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.DeliveryRecord;

/**
 * delivery_record 表的 MyBatis-Plus mapper，供骑手接单、取餐和送达服务使用。
 * 状态变更应使用条件更新防止重复接单，并与订单状态在同一事务中推进。
 */
public interface DeliveryRecordMapper extends BaseMapper<DeliveryRecord> {
}
