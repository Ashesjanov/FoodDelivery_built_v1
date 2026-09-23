package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.CartItem;

/**
 * cart_item 表的 MyBatis-Plus mapper，为购物车服务提供通用 CRUD。
 * 实现由 MapperScan 注册；多条购物车变更应由 service 在事务内协调。
 */
public interface CartItemMapper extends BaseMapper<CartItem> {
}
