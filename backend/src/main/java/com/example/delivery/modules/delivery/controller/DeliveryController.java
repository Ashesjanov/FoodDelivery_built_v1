package com.example.delivery.modules.delivery.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.common.PageResponse;
import com.example.delivery.modules.delivery.dto.DeliveryDto;
import com.example.delivery.modules.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 配送 REST 控制器：提供 /api/delivery 下的骑手资料、位置、状态和接单履约入口。
 * 仅依赖 {@link DeliveryService}；类级限制 RIDER 角色，控制器不管理事务，
 * 并发接单、状态推进和骑手数据隔离均在服务层处理。
 */
@Tag(name = "Delivery")
@RestController
@RequestMapping("/api/delivery")
@PreAuthorize("hasRole('RIDER')")
public class DeliveryController {
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/riders/me")
    public ApiResponse<DeliveryDto.RiderView> rider() {
        return ApiResponse.ok(deliveryService.myRider());
    }

    @PutMapping("/riders/me")
    public ApiResponse<DeliveryDto.RiderView> updateRider(@Valid @RequestBody DeliveryDto.RiderProfileRequest request) {
        return ApiResponse.ok(deliveryService.updateProfile(request));
    }

    @PutMapping("/riders/me/status")
    public ApiResponse<DeliveryDto.RiderView> updateStatus(
            @Valid @RequestBody DeliveryDto.RiderStatusRequest request) {
        return ApiResponse.ok(deliveryService.updateStatus(request));
    }

    @PutMapping("/riders/me/location")
    public ApiResponse<DeliveryDto.RiderView> updateLocation(
            @Valid @RequestBody DeliveryDto.LocationRequest request) {
        return ApiResponse.ok(deliveryService.updateLocation(request));
    }

    @GetMapping("/orders/available")
    public ApiResponse<PageResponse<DeliveryDto.View>> available(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(deliveryService.available(page, size));
    }

    @PostMapping("/orders/{id}/accept")
    public ApiResponse<DeliveryDto.View> accept(@PathVariable("id") Long id) {
        return ApiResponse.ok(deliveryService.accept(id));
    }

    @PostMapping("/orders/{id}/pickup")
    public ApiResponse<DeliveryDto.View> pickup(@PathVariable("id") Long id,
                                                @Valid @RequestBody(required = false)
                                                DeliveryDto.DeliveryNoteRequest request) {
        return ApiResponse.ok(deliveryService.pickup(id, request));
    }

    @PostMapping("/orders/{id}/deliver")
    public ApiResponse<DeliveryDto.View> deliver(@PathVariable("id") Long id,
                                                 @Valid @RequestBody(required = false)
                                                 DeliveryDto.DeliveryNoteRequest request) {
        return ApiResponse.ok(deliveryService.deliver(id, request));
    }

    @GetMapping("/orders/active")
    public ApiResponse<List<DeliveryDto.View>> active() {
        return ApiResponse.ok(deliveryService.active());
    }

    @GetMapping("/orders/{id}/delivery")
    public ApiResponse<DeliveryDto.View> delivery(@PathVariable("id") Long id) {
        return ApiResponse.ok(deliveryService.record(id));
    }

    @GetMapping("/records")
    public ApiResponse<PageResponse<DeliveryDto.View>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(deliveryService.records(page, size));
    }
}
