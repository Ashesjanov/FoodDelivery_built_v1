package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.UserAddress;

/**
 * user_address 表的 MyBatis-Plus mapper，供用户收货地址维护和下单快照查询使用。
 * 默认地址唯一性由地址服务在事务内维护，订单保存的是地址快照而非实时关联。
 */
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
