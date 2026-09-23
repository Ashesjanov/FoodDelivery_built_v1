package com.example.delivery.modules.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.entity.Merchant;
import com.example.delivery.domain.entity.Orders;
import com.example.delivery.domain.entity.Review;
import com.example.delivery.domain.enums.OrderStatus;
import com.example.delivery.domain.enums.ReviewStatus;
import com.example.delivery.domain.mapper.MerchantMapper;
import com.example.delivery.domain.mapper.OrdersMapper;
import com.example.delivery.domain.mapper.ReviewMapper;
import com.example.delivery.modules.review.dto.ReviewDto;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 评价服务：处理已送达订单的评价、商家回复以及商家评分重算。
 * 依赖评价/订单/商家 Mapper；创建和回复在事务内并校验客户或商家所有者身份，
 * 公开查询只返回可见评价，一单一条评价的规则在创建时检查。
 */
@Service
public class ReviewService {
    private static final int MAX_PAGE_SIZE = 200;

    private final ReviewMapper reviewMapper;
    private final OrdersMapper ordersMapper;
    private final MerchantMapper merchantMapper;

    public ReviewService(ReviewMapper reviewMapper, OrdersMapper ordersMapper, MerchantMapper merchantMapper) {
        this.reviewMapper = reviewMapper;
        this.ordersMapper = ordersMapper;
        this.merchantMapper = merchantMapper;
    }

    @Transactional
    public ReviewDto.View create(ReviewDto.CreateRequest request) {
        long userId = SecurityUtils.requireCurrentUserId();
        Orders order = ordersMapper.selectById(request.orderId());
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "reviewed order was not found");
        }
        if (!SecurityUtils.hasRole("ADMIN") && !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ErrorCode.ACCESS_DENIED, "only the order customer can create a review");
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BizException(ErrorCode.CONFLICT, "an order can be reviewed only after delivery");
        }
        // 一单最多一条评价：先返回友好冲突，数据库 order_id 唯一约束兜底并发插入。
        Long existing = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId()));
        if (existing != null && existing > 0) {
            throw new BizException(ErrorCode.CONFLICT, "this order has already been reviewed");
        }
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        if (merchant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "reviewed merchant was not found");
        }

        LocalDateTime now = LocalDateTime.now();
        Review review = Review.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .merchantId(order.getMerchantId())
                .rating(request.rating())
                .content(request.content())
                .imageUrl(request.imageUrl())
                .status(ReviewStatus.VISIBLE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        reviewMapper.insert(review);
        updateMerchantRating(merchant, now);
        return view(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto.View> merchantReviews(Long merchantId, int page, int size) {
        if (merchantMapper.selectById(merchantId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "merchant was not found");
        }
        int current = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Review> result = reviewMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getMerchantId, merchantId)
                        .eq(Review::getStatus, ReviewStatus.VISIBLE)
                        .orderByDesc(Review::getCreatedAt)
                        .orderByDesc(Review::getId));
        return PageResponse.of(result.getRecords().stream().map(ReviewService::view).toList(),
                result.getTotal(), current, pageSize);
    }

    @Transactional
    public ReviewDto.View reply(Long reviewId, ReviewDto.ReplyRequest request) {
        long userId = SecurityUtils.requireCurrentUserId();
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "review was not found");
        }
        Merchant merchant = merchantMapper.selectById(review.getMerchantId());
        if (merchant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "reviewed merchant was not found");
        }
        if (!SecurityUtils.hasRole("ADMIN") && !Objects.equals(merchant.getOwnerId(), userId)) {
            throw new BizException(ErrorCode.ACCESS_DENIED, "only the merchant owner can reply to this review");
        }

        LocalDateTime now = LocalDateTime.now();
        String reply = request.replyContent() == null || request.replyContent().isBlank()
                ? null : request.replyContent().trim();
        review.setReplyContent(reply);
        review.setRepliedAt(reply == null ? null : now);
        review.setUpdatedAt(now);
        reviewMapper.updateById(review);
        return view(review);
    }

    private void updateMerchantRating(Merchant merchant, LocalDateTime now) {
        // 评分只基于可见评价在事务内重算，新增评价会同步刷新商家聚合评分。
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchant.getId())
                .eq(Review::getStatus, ReviewStatus.VISIBLE));
        BigDecimal rating = reviews.stream()
                .map(review -> BigDecimal.valueOf(review.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(reviews.size(), 1)), 2, RoundingMode.HALF_UP);
        merchant.setRating(rating);
        merchant.setUpdatedAt(now);
        merchantMapper.updateById(merchant);
    }

    private static ReviewDto.View view(Review review) {
        return new ReviewDto.View(review.getId(), review.getOrderId(), review.getUserId(), review.getMerchantId(),
                review.getRating(), review.getContent(), review.getImageUrl(), review.getReplyContent(),
                review.getRepliedAt(), review.getCreatedAt());
    }
}
