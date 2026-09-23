package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.Orders;

/**
 * orders 表的单数命名兼容 mapper，为使用 OrderMapper 的订单服务提供 BaseMapper CRUD。
 * 与 OrdersMapper 操作同一实体；新代码应在一次业务中注入唯一 mapper，避免双写歧义。
 */
public interface OrderMapper extends BaseMapper<Orders> {
}
