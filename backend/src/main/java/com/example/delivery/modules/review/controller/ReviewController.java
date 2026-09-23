package com.example.delivery.modules.review.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.common.PageResponse;
import com.example.delivery.modules.review.dto.ReviewDto;
import com.example.delivery.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价 REST 控制器：提供 /api/reviews 下的创建评价、查看商家评价和商家回复入口。
 * 仅依赖 {@link ReviewService}；类级要求登录，创建限客户/管理员，回复限商家/管理员，
 * 控制器不开启事务，订单归属和商家所有者校验由服务层执行。
 */
@Tag(name = "Reviews")
@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("isAuthenticated()")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReviewDto.View>> create(
            @Valid @RequestBody ReviewDto.CreateRequest request) {
        ReviewDto.View review = reviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(review));
    }

    @GetMapping("/merchants/{merchantId}")
    public ApiResponse<PageResponse<ReviewDto.View>> merchantReviews(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.merchantReviews(merchantId, page, size));
    }

    @PostMapping("/{id}/reply")
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ApiResponse<ReviewDto.View> reply(@PathVariable("id") Long id,
                                             @Valid @RequestBody ReviewDto.ReplyRequest request) {
        return ApiResponse.ok(reviewService.reply(id, request));
    }
}
