package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.CouponClaim;

/**
 * coupon_claim 表的 MyBatis-Plus mapper，为领券和用券服务提供持久化能力。
 * 核销并发由 service 的事务和状态条件控制，不应在 mapper 外单独修改状态。
 */
public interface CouponClaimMapper extends BaseMapper<CouponClaim> {
}
