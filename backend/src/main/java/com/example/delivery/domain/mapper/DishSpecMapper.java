package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.DishSpec;

/**
 * dish_spec 表的 MyBatis-Plus mapper，维护菜品规格和加价项。
 * 服务通常在加载菜品时联查规格；规格变更不应破坏已下单订单的规格快照。
 */
public interface DishSpecMapper extends BaseMapper<DishSpec> {
}
