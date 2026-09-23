package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.Orders;

/**
 * orders 表的 MyBatis-Plus mapper，为订单主表查询和状态流转提供持久化能力。
 * 订单状态、支付和配送写入必须由 service 在同一事务协调，避免跨表状态漂移。
 */
public interface OrdersMapper extends BaseMapper<Orders> {
}
