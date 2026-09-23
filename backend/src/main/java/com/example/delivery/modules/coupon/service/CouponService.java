package com.example.delivery.modules.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.Coupon;
import com.example.delivery.domain.entity.CouponClaim;
import com.example.delivery.domain.enums.CouponClaimStatus;
import com.example.delivery.domain.enums.CouponStatus;
import com.example.delivery.domain.enums.CouponType;
import com.example.delivery.domain.mapper.CouponClaimMapper;
import com.example.delivery.domain.mapper.CouponMapper;
import com.example.delivery.modules.coupon.dto.CouponDto;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理优惠券活动、用户领取、订单折扣计算和领取记录核销。
 * 依赖 CouponMapper、CouponClaimMapper；创建、领取、核销和回滚均为事务操作，
 * 角色及当前用户校验由 {@link SecurityUtils} 提供。
 */
@Service
public class CouponService {
    private final CouponMapper couponMapper;
    private final CouponClaimMapper claimMapper;
    public CouponService(CouponMapper couponMapper, CouponClaimMapper claimMapper) { this.couponMapper = couponMapper; this.claimMapper = claimMapper; }

    public List<CouponDto.CouponView> available() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>().le(Coupon::getStartTime, now).ge(Coupon::getEndTime, now)
                .ne(Coupon::getStatus, CouponStatus.EXHAUSTED).orderByDesc(Coupon::getDiscountAmount)).stream().map(this::view).toList();
    }

    @Transactional
    public CouponDto.CouponView create(CouponDto.CreateRequest request) {
        if (!SecurityUtils.hasAnyRole("ADMIN", "MERCHANT")) throw new BizException(ErrorCode.ACCESS_DENIED);
        if (!request.endTime().isAfter(request.startTime())) throw new BizException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        if (request.couponType() == CouponType.FIXED && request.discountAmount() == null) throw new BizException(ErrorCode.BAD_REQUEST, "固定金额优惠券必须设置优惠金额");
        Coupon coupon = new Coupon(); coupon.setName(request.name()); coupon.setCode(request.code()); coupon.setCouponType(request.couponType());
        // 新活动初始为 UPCOMING；ACTIVE、PAUSED、EXHAUSTED 由领取或运营流程推进。
        coupon.setStatus(CouponStatus.UPCOMING); coupon.setThresholdAmount(request.thresholdAmount()); coupon.setDiscountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        coupon.setDiscountRate(request.discountRate()); coupon.setTotalQuantity(request.totalQuantity()); coupon.setClaimedQuantity(0); coupon.setPerUserLimit(request.perUserLimit() == null ? 1 : request.perUserLimit());
        coupon.setStartTime(request.startTime()); coupon.setEndTime(request.endTime()); couponMapper.insert(coupon); return view(coupon);
    }

    @Transactional
    public CouponDto.ClaimView claim(Long couponId) {
        long userId = SecurityUtils.requireCurrentUserId(); LocalDateTime now = LocalDateTime.now(); Coupon coupon = requireCoupon(couponId);
        if (coupon.getStartTime().isAfter(now) || coupon.getEndTime().isBefore(now) || coupon.getStatus() == CouponStatus.PAUSED) throw new BizException(ErrorCode.CONFLICT, "优惠券当前不可领取");
        Long claimed = claimMapper.selectCount(new LambdaQueryWrapper<CouponClaim>().eq(CouponClaim::getCouponId, couponId).eq(CouponClaim::getUserId, userId)
                .in(CouponClaim::getStatus, CouponClaimStatus.UNUSED, CouponClaimStatus.USED));
        if (claimed >= coupon.getPerUserLimit()) throw new BizException(ErrorCode.CONFLICT, "已达到领取上限");
        // 条件更新同时校验 ACTIVE 和剩余量，避免并发超领。
        int updated = couponMapper.update(null, new LambdaUpdateWrapper<Coupon>().eq(Coupon::getId, couponId)
                .eq(Coupon::getStatus, CouponStatus.ACTIVE).apply("claimed_quantity < total_quantity")
                .setSql("claimed_quantity = claimed_quantity + 1"));
        if (updated == 0) throw new BizException(ErrorCode.CONFLICT, "优惠券已领完");
        Coupon fresh = requireCoupon(couponId);
        // 最后一张被领走后，将活动推进为 EXHAUSTED。
        if (fresh.getClaimedQuantity() >= fresh.getTotalQuantity()) { fresh.setStatus(CouponStatus.EXHAUSTED); couponMapper.updateById(fresh); }
        CouponClaim claim = new CouponClaim(); claim.setCouponId(couponId); claim.setUserId(userId); claim.setStatus(CouponClaimStatus.UNUSED);
        // 预占成功后创建用户持有的 UNUSED 记录，并随活动结束时间失效。
        claim.setClaimedAt(now); claim.setExpiresAt(coupon.getEndTime()); claimMapper.insert(claim); return claimView(claim, coupon);
    }

    public List<CouponDto.ClaimView> my(CouponClaimStatus status) {
        long userId = SecurityUtils.requireCurrentUserId();
        return claimMapper.selectList(new LambdaQueryWrapper<CouponClaim>().eq(CouponClaim::getUserId, userId).eq(status != null, CouponClaim::getStatus, status)
                .orderByDesc(CouponClaim::getClaimedAt)).stream().map(c -> claimView(c, requireCoupon(c.getCouponId()))).toList();
    }

    public BigDecimal calculateDiscount(long userId, Long claimId, BigDecimal subtotal) {
        CouponClaim claim = claimMapper.selectById(claimId);
        if (claim == null || !claim.getUserId().equals(userId) || claim.getStatus() != CouponClaimStatus.UNUSED || claim.getExpiresAt().isBefore(LocalDateTime.now())) throw new BizException(ErrorCode.CONFLICT, "优惠券不可用");
        Coupon coupon = requireCoupon(claim.getCouponId());
        if (subtotal.compareTo(coupon.getThresholdAmount()) < 0) throw new BizException(ErrorCode.CONFLICT, "未达到优惠券门槛");
        // FIXED 优惠不超过小计；折扣率不大于 1 按成交比例解释，否则按百分比解释。
        if (coupon.getCouponType() == CouponType.FIXED) return coupon.getDiscountAmount().min(subtotal);
        BigDecimal rate = coupon.getDiscountRate();
        BigDecimal discount = rate != null && rate.compareTo(BigDecimal.ONE) <= 0 ? subtotal.multiply(BigDecimal.ONE.subtract(rate)) : subtotal.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void consumeClaim(Long claimId, Long orderId) {
        // 仅允许 UNUSED 到 USED 的条件迁移，避免同一领取记录为多笔订单重复抵扣。
        int updated = claimMapper.update(null, new LambdaUpdateWrapper<CouponClaim>().eq(CouponClaim::getId, claimId)
                .eq(CouponClaim::getStatus, CouponClaimStatus.UNUSED).set(CouponClaim::getStatus, CouponClaimStatus.USED)
                .set(CouponClaim::getOrderId, orderId).set(CouponClaim::getUsedAt, LocalDateTime.now()));
        if (updated == 0) throw new BizException(ErrorCode.CONFLICT, "优惠券已被使用");
    }

    @Transactional
    public void restoreClaim(Long claimId) {
        if (claimId == null) return;
        // 订单失败回滚时，只释放由该流程已核销的领取记录。
        claimMapper.update(null, new LambdaUpdateWrapper<CouponClaim>().eq(CouponClaim::getId, claimId)
                .eq(CouponClaim::getStatus, CouponClaimStatus.USED).set(CouponClaim::getStatus, CouponClaimStatus.UNUSED)
                .set(CouponClaim::getOrderId, null).set(CouponClaim::getUsedAt, null));
    }

    private Coupon requireCoupon(Long id) { Coupon coupon = couponMapper.selectById(id); if (coupon == null) throw new BizException(ErrorCode.NOT_FOUND); return coupon; }
    private CouponDto.CouponView view(Coupon c) { return new CouponDto.CouponView(c.getId(), c.getName(), c.getCode(), c.getCouponType(), c.getStatus(), c.getThresholdAmount(), c.getDiscountAmount(), c.getDiscountRate(), c.getTotalQuantity(), c.getClaimedQuantity(), Math.max(0, c.getTotalQuantity() - c.getClaimedQuantity()), c.getPerUserLimit(), c.getStartTime(), c.getEndTime()); }
    private CouponDto.ClaimView claimView(CouponClaim c, Coupon coupon) { return new CouponDto.ClaimView(c.getId(), c.getCouponId(), c.getOrderId(), c.getStatus(), coupon.getName(), coupon.getCouponType(), coupon.getThresholdAmount(), coupon.getDiscountAmount(), coupon.getDiscountRate(), c.getClaimedAt(), c.getUsedAt(), c.getExpiresAt()); }
}
