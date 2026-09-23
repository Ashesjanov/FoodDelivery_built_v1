package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.Coupon;

/**
 * coupon 表的 MyBatis-Plus mapper，供优惠活动管理和领取资格校验查询。
 * 实现由 MapperScan 生成；活动库存更新需与 coupon_claim 写入保持事务一致。
 */
public interface CouponMapper extends BaseMapper<Coupon> {
}
