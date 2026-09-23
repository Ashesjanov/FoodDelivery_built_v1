package com.example.delivery.modules.order.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.enums.OrderStatus;
import com.example.delivery.modules.order.dto.OrderDto;
import com.example.delivery.modules.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 REST 控制器：提供 /api/orders 下的创建、查询、取消和履约状态推进入口。
 * 仅依赖 {@link OrderService}，请求参数由 Bean Validation 校验；控制器不管理事务，
 * 写操作的事务边界以及客户、商家、骑手的数据归属检查均由服务层执行。
 */
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto.Detail>> create(@Valid @RequestBody OrderDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(orderService.create(request)));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderDto.View>> page(@RequestParam(required = false) Long merchantId,
                                                         @RequestParam(required = false) Long userId,
                                                         @RequestParam(required = false) OrderStatus status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.page(merchantId, userId, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDto.Detail> get(@PathVariable Long id) {
        return ApiResponse.ok(orderService.get(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDto.Detail> cancel(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) OrderDto.ReasonRequest request) {
        return ApiResponse.ok(orderService.cancel(id, request == null ? new OrderDto.ReasonRequest(null) : request));
    }

    @PostMapping("/{id}/user-cancel")
    public ApiResponse<OrderDto.Detail> userCancel(@PathVariable Long id,
                                                   @Valid @RequestBody(required = false) OrderDto.ReasonRequest request) {
        return ApiResponse.ok(orderService.cancelByUser(id, request == null ? new OrderDto.ReasonRequest(null) : request));
    }

    @PostMapping("/{id}/merchant-cancel")
    public ApiResponse<OrderDto.Detail> merchantCancel(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false) OrderDto.ReasonRequest request) {
        return ApiResponse.ok(orderService.cancelByMerchant(id, request == null ? new OrderDto.ReasonRequest(null) : request));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<OrderDto.Detail> accept(@PathVariable Long id) {
        return ApiResponse.ok(orderService.accept(id));
    }

    @PostMapping("/{id}/ready")
    public ApiResponse<OrderDto.Detail> ready(@PathVariable Long id) {
        return ApiResponse.ok(orderService.ready(id));
    }

    @PostMapping("/{id}/pickup")
    public ApiResponse<OrderDto.Detail> pickup(@PathVariable Long id) {
        return ApiResponse.ok(orderService.pickup(id));
    }

    @PostMapping("/{id}/deliver")
    public ApiResponse<OrderDto.Detail> deliver(@PathVariable Long id) {
        return ApiResponse.ok(orderService.deliver(id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderDto.Detail> complete(@PathVariable Long id) {
        return ApiResponse.ok(orderService.complete(id));
    }
}
