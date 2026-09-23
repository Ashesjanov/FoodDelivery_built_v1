package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.OrderItem;

/**
 * order_item 表的 MyBatis-Plus mapper，持久化订单菜品和规格快照。
 * 明细应与订单在同一事务写入，历史明细不可跟随菜品档案变化而改写。
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
