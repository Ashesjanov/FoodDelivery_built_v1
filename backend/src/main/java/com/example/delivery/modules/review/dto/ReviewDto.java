package com.example.delivery.modules.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 评价模块的请求和响应模型：包含订单评价、商家回复和公开评价视图。
 * 仅承担 Bean Validation 和传输职责，不包含唯一性或授权规则，也不涉及事务边界。
 */
public final class ReviewDto {
    private ReviewDto() {
    }

    public record CreateRequest(@NotNull Long orderId, @NotNull @Min(1) @Max(5) Integer rating,
                                @Size(max = 500) String content, @Size(max = 255) String imageUrl) {
    }

    public record ReplyRequest(@Size(max = 500) String replyContent) {
    }

    public record View(Long id, Long orderId, Long userId, Long merchantId, Integer rating, String content,
                       String imageUrl, String replyContent, LocalDateTime repliedAt, LocalDateTime createdAt) {
    }
}
