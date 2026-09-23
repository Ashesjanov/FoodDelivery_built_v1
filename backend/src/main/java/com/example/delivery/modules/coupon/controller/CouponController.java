package com.example.delivery.modules.coupon.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.domain.enums.CouponClaimStatus;
import com.example.delivery.modules.coupon.dto.CouponDto;
import com.example.delivery.modules.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 优惠券 REST 控制器，统一入口为 {@code /api/coupons}。
 * 提供活动查询、创建、领取和个人领取记录；REST 边界负责字段校验，
 * {@link CouponService} 负责角色、时间、领取上限和优惠券状态规则。
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService service;
    public CouponController(CouponService service) { this.service = service; }
    @GetMapping public ApiResponse<List<CouponDto.CouponView>> available() { return ApiResponse.ok(service.available()); }
    @PostMapping public ApiResponse<CouponDto.CouponView> create(@Valid @RequestBody CouponDto.CreateRequest request) { return ApiResponse.created(service.create(request)); }
    @PostMapping("/{id}/claim") public ApiResponse<CouponDto.ClaimView> claim(@PathVariable Long id) { return ApiResponse.created(service.claim(id)); }
    @GetMapping("/my") public ApiResponse<List<CouponDto.ClaimView>> my(@RequestParam(required = false) CouponClaimStatus status) { return ApiResponse.ok(service.my(status)); }
}
