package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.Dish;

/**
 * dish 表的 MyBatis-Plus mapper，供商户菜单管理和点餐查询使用。
 * 上下架、库存和销量属于业务状态，统一由菜品/订单服务在事务中修改。
 */
public interface DishMapper extends BaseMapper<Dish> {
}
