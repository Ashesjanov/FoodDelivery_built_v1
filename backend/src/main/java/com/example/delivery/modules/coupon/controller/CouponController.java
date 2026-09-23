package com.example.delivery.modules.coupon.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.domain.enums.CouponClaimStatus;
import com.example.delivery.modules.coupon.dto.CouponDto;
import com.example.delivery.modules.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
