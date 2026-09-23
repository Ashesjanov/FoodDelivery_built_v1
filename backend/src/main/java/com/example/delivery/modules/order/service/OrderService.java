package com.example.delivery.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.entity.CartItem;
import com.example.delivery.domain.entity.Dish;
import com.example.delivery.domain.entity.DishSpec;
import com.example.delivery.domain.entity.Merchant;
import com.example.delivery.domain.entity.OrderItem;
import com.example.delivery.domain.entity.Orders;
import com.example.delivery.domain.entity.PaymentRecord;
import com.example.delivery.domain.entity.UserAddress;
import com.example.delivery.domain.enums.CartItemStatus;
import com.example.delivery.domain.enums.DishStatus;
import com.example.delivery.domain.enums.MerchantBusinessStatus;
import com.example.delivery.domain.enums.OrderStatus;
import com.example.delivery.domain.enums.PaymentStatus;
import com.example.delivery.domain.mapper.CartItemMapper;
import com.example.delivery.domain.mapper.DishMapper;
import com.example.delivery.domain.mapper.DishSpecMapper;
import com.example.delivery.domain.mapper.MerchantMapper;
import com.example.delivery.domain.mapper.OrderItemMapper;
import com.example.delivery.domain.mapper.OrderMapper;
import com.example.delivery.domain.mapper.PaymentRecordMapper;
import com.example.delivery.domain.mapper.UserAddressMapper;
import com.example.delivery.modules.coupon.service.CouponService;
import com.example.delivery.modules.order.dto.OrderDto;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 订单核心服务：负责购物车创建订单、价格计算、订单状态机、查询授权和取消补偿。
 * 依赖订单/明细/购物车/菜品 Mapper、优惠券服务和 {@link OrderPushService}；写方法均在事务内，
 * 调用方必须已登录，客户、商家和骑手只能访问各自授权范围内的订单。
 */
@Service
public class OrderService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;
    private final MerchantMapper merchantMapper;
    private final UserAddressMapper userAddressMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CouponService couponService;
    private final OrderPushService orderPushService;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper, CartItemMapper cartItemMapper,
                        DishMapper dishMapper, DishSpecMapper dishSpecMapper, MerchantMapper merchantMapper,
                        UserAddressMapper userAddressMapper, PaymentRecordMapper paymentRecordMapper,
                        CouponService couponService, OrderPushService orderPushService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartItemMapper = cartItemMapper;
        this.dishMapper = dishMapper;
        this.dishSpecMapper = dishSpecMapper;
        this.merchantMapper = merchantMapper;
        this.userAddressMapper = userAddressMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.couponService = couponService;
        this.orderPushService = orderPushService;
    }

    @Transactional
    public OrderDto.Detail create(OrderDto.CreateRequest request) {
        long userId = SecurityUtils.requireCurrentUserId();
        List<CartItem> cartItems = loadCartItems(userId, request.cartItemIds());
        if (cartItems.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "cart items are required");
        }

        Long merchantId = cartItems.getFirst().getMerchantId();
        if (cartItems.stream().anyMatch(item -> !Objects.equals(item.getMerchantId(), merchantId))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "one order may only contain one merchant");
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (merchant.getBusinessStatus() != MerchantBusinessStatus.OPEN) {
            throw new BizException(ErrorCode.CONFLICT, "merchant is not open");
        }

        UserAddress address = userAddressMapper.selectById(request.addressId());
        if (address == null || !Objects.equals(address.getUserId(), userId)) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        // 订单明细保存下单时的商品名称和价格快照，后续菜品改价不影响既有订单。
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> stockDemand = new HashMap<>();
        BigDecimal totalAmount = ZERO;
        for (CartItem cartItem : cartItems) {
            Dish dish = dishMapper.selectById(cartItem.getDishId());
            if (dish == null || dish.getStatus() != DishStatus.ON_SALE
                    || !Objects.equals(dish.getMerchantId(), merchantId)) {
                throw new BizException(ErrorCode.CONFLICT, "a cart item is no longer purchasable");
            }
            DishSpec spec = loadSpec(cartItem.getDishSpecId(), dish.getId());
            int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            if (quantity < 1) {
                throw new BizException(ErrorCode.BAD_REQUEST, "quantity must be positive");
            }
            stockDemand.merge(dish.getId(), quantity, Integer::sum);

            BigDecimal unitPrice = dish.getPrice()
                    .add(spec == null || spec.getPriceOffset() == null ? BigDecimal.ZERO : spec.getPriceOffset())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);
            orderItems.add(OrderItem.builder().dishId(dish.getId()).dishSpecId(spec == null ? null : spec.getId())
                    .dishName(dish.getName()).specName(spec == null ? null : spec.getName())
                    .unitPrice(unitPrice).quantity(quantity).subtotal(subtotal).note(cartItem.getNote()).build());
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        if (merchant.getMinOrderAmount() != null && totalAmount.compareTo(merchant.getMinOrderAmount()) < 0) {
            throw new BizException(ErrorCode.CONFLICT, "minimum order amount is not reached");
        }

        // 金额统一保留两位；优惠不能超过商品金额，最终应付金额不得为负。
        BigDecimal discountAmount = request.couponClaimId() == null ? ZERO
                : nvl(couponService.calculateDiscount(userId, request.couponClaimId(), totalAmount));
        if (discountAmount.compareTo(totalAmount) > 0) {
            discountAmount = totalAmount;
        }
        BigDecimal deliveryFee = nvl(merchant.getDeliveryFee());
        BigDecimal packagingFee = nvl(merchant.getPackagingFee());
        BigDecimal payableAmount = totalAmount.add(deliveryFee).add(packagingFee).subtract(discountAmount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        Orders order = Orders.builder().orderNo(newOrderNo()).userId(userId).merchantId(merchantId)
                .addressId(address.getId()).couponClaimId(request.couponClaimId()).status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount).deliveryFee(deliveryFee).packagingFee(packagingFee)
                .discountAmount(discountAmount).payableAmount(payableAmount).userNote(trim(request.userNote()))
                .contactPhone(request.contactPhone() == null || request.contactPhone().isBlank()
                        ? address.getPhone() : request.contactPhone().trim())
                .deliveryAddressSnapshot(addressSnapshot(address)).createdAt(now).updatedAt(now).build();
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        // 扣库存、核销优惠券和锁定购物车与订单创建同事务执行，任一步失败都会整体回滚。
        reserveStock(stockDemand);
        if (request.couponClaimId() != null) {
            couponService.consumeClaim(request.couponClaimId(), order.getId());
        }
        markCartCheckedOut(cartItems);
        // 经由 /topic/orders/{orderId} 在事务提交后通知订阅端，避免客户端看到被回滚的创建事件。
        orderPushService.publishAfterCommit(order.getId(), order.getStatus().getCode(), "order created, waiting for payment");
        return detail(order.getId(), order, orderItems);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto.View> page(Long merchantId, Long userId, OrderStatus status, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 200);
        long currentUserId = SecurityUtils.requireCurrentUserId();
        boolean admin = SecurityUtils.hasRole("ADMIN");
        boolean merchantRole = SecurityUtils.hasRole("MERCHANT");

        if (!admin && !merchantRole) {
            userId = currentUserId;
        } else if (merchantRole) {
            if (merchantId != null) {
                requireOwnedMerchant(merchantId);
            } else {
                List<Long> merchantIds = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getOwnerId, currentUserId)).stream().map(Merchant::getId).toList();
                if (merchantIds.isEmpty()) {
                    return PageResponse.empty(pageNo, pageSize);
                }
                Page<Orders> emptyCheck = orderMapper.selectPage(new Page<>(pageNo, pageSize),
                        new LambdaQueryWrapper<Orders>().in(Orders::getMerchantId, merchantIds)
                                .eq(status != null, Orders::getStatus, status).orderByDesc(Orders::getCreatedAt));
                return pageResponse(emptyCheck, pageNo, pageSize);
            }
        }

        Page<Orders> result = orderMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Orders>().eq(merchantId != null, Orders::getMerchantId, merchantId)
                        .eq(userId != null, Orders::getUserId, userId)
                        .eq(status != null, Orders::getStatus, status).orderByDesc(Orders::getCreatedAt));
        return pageResponse(result, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public OrderDto.Detail get(Long id) {
        Orders order = requireVisibleOrder(id);
        return detail(order.getId(), order, orderItems(order.getId()));
    }

    @Transactional
    public OrderDto.Detail cancel(Long id, OrderDto.ReasonRequest request) {
        return SecurityUtils.hasAnyRole("MERCHANT", "ADMIN")
                ? cancelByMerchant(id, request) : cancelByUser(id, request);
    }

    @Transactional
    public OrderDto.Detail cancelByUser(Long id, OrderDto.ReasonRequest request) {
        Orders order = requireOwnedOrder(id);
        return cancelInternal(order, request, false);
    }

    @Transactional
    public OrderDto.Detail cancelByMerchant(Long id, OrderDto.ReasonRequest request) {
        Orders order = requireMerchantOrder(id);
        return cancelInternal(order, request, true);
    }

    @Transactional
    public OrderDto.Detail accept(Long id) {
        Orders order = requireMerchantOrder(id);
        Orders updated = transition(order, List.of(OrderStatus.PAID), List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING,
                OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                OrderStatus.ACCEPTED, "acceptedAt");
        orderPushService.publishAfterCommit(id, updated.getStatus().getCode(), "merchant accepted the order");
        return detail(id, updated, orderItems(id));
    }

    @Transactional
    public OrderDto.Detail ready(Long id) {
        Orders order = requireMerchantOrder(id);
        Orders updated = transition(order, List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING),
                List.of(OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                OrderStatus.READY, "readyAt");
        orderPushService.publishAfterCommit(id, updated.getStatus().getCode(), "order is ready for pickup");
        return detail(id, updated, orderItems(id));
    }

    /** PICKED_UP 是共享订单状态枚举中对“配送中（DELIVERING）”阶段的表示。 */
    @Transactional
    public OrderDto.Detail pickup(Long id) {
        Orders order = requireDeliveryActor(id);
        Orders updated = transition(order, List.of(OrderStatus.READY),
                List.of(OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                OrderStatus.PICKED_UP, "pickedAt");
        orderPushService.publishAfterCommit(id, updated.getStatus().getCode(), "order is delivering");
        return detail(id, updated, orderItems(id));
    }

    @Transactional
    public OrderDto.Detail deliver(Long id) {
        Orders order = requireDeliveryActor(id);
        Orders updated = transition(order, List.of(OrderStatus.PICKED_UP), List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                OrderStatus.DELIVERED, "deliveredAt");
        orderPushService.publishAfterCommit(id, updated.getStatus().getCode(), "order delivered");
        return detail(id, updated, orderItems(id));
    }

    @Transactional
    public OrderDto.Detail complete(Long id) {
        Orders order = requireOwnedOrder(id);
        Orders updated = transition(order, List.of(OrderStatus.DELIVERED), List.of(OrderStatus.COMPLETED),
                OrderStatus.COMPLETED, null);
        orderPushService.publishAfterCommit(id, updated.getStatus().getCode(), "order completed");
        return detail(id, updated, orderItems(id));
    }

    @Transactional
    public void markPaid(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        Orders updated = transition(order, List.of(OrderStatus.PENDING_PAYMENT), List.of(OrderStatus.PAID),
                OrderStatus.PAID, null);
        orderPushService.publishAfterCommit(orderId, updated.getStatus().getCode(), "payment succeeded");
    }

    private OrderDto.Detail cancelInternal(Orders order, OrderDto.ReasonRequest request, boolean merchantAction) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return detail(order.getId(), order, orderItems(order.getId()));
        }
        if (!merchantAction && Set.of(OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERED,
                OrderStatus.COMPLETED).contains(order.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "order can no longer be cancelled");
        }
        if (!Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.PREPARING)
                .contains(order.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "order status does not allow cancellation");
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? (merchantAction ? "cancelled by merchant" : "cancelled by user") : request.reason().trim();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Orders>().eq(Orders::getId, order.getId())
                .in(Orders::getStatus, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.ACCEPTED,
                        OrderStatus.PREPARING)
                .set(Orders::getStatus, OrderStatus.CANCELLED).set(Orders::getCancelReason, reason)
                .set(Orders::getCancelledAt, now).set(Orders::getUpdatedAt, now));
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "order changed concurrently");
        }

        // 取消补偿：恢复库存和优惠券；已支付记录全额退款，待支付记录关闭。
        releaseStock(orderItems(order.getId()));
        couponService.restoreClaim(order.getCouponClaimId());
        settlePaymentOnCancellation(order.getId(), reason);
        Orders fresh = orderMapper.selectById(order.getId());
        orderPushService.publishAfterCommit(order.getId(), fresh.getStatus().getCode(), reason);
        return detail(order.getId(), fresh, orderItems(order.getId()));
    }

    private Orders transition(Orders loaded, List<OrderStatus> expected, List<OrderStatus> idempotent,
                              OrderStatus target, String timestampField) {
        // 重复推进到后续状态时按幂等成功返回；其他情况用期望状态作为条件更新，防止并发越级。
        if (idempotent.contains(loaded.getStatus())) {
            return loaded;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Orders> update = new LambdaUpdateWrapper<Orders>().eq(Orders::getId, loaded.getId())
                .in(Orders::getStatus, expected).set(Orders::getStatus, target).set(Orders::getUpdatedAt, now);
        if ("acceptedAt".equals(timestampField)) update.set(Orders::getAcceptedAt, now);
        if ("readyAt".equals(timestampField)) update.set(Orders::getReadyAt, now);
        if ("pickedAt".equals(timestampField)) update.set(Orders::getPickedAt, now);
        if ("deliveredAt".equals(timestampField)) update.set(Orders::getDeliveredAt, now);
        if (orderMapper.update(null, update) == 0) {
            Orders fresh = orderMapper.selectById(loaded.getId());
            if (fresh != null && idempotent.contains(fresh.getStatus())) return fresh;
            throw new BizException(ErrorCode.CONFLICT, "invalid order status transition");
        }
        return orderMapper.selectById(loaded.getId());
    }

    private void reserveStock(Map<Long, Integer> demand) {
        // 条件更新同时检查上架状态和剩余库存，零行更新表示竞争失败并触发回滚。
        for (Map.Entry<Long, Integer> entry : demand.entrySet()) {
            int quantity = entry.getValue();
            int updated = dishMapper.update(null, new LambdaUpdateWrapper<Dish>().eq(Dish::getId, entry.getKey())
                    .eq(Dish::getStatus, DishStatus.ON_SALE).ge(Dish::getStock, quantity)
                    .setSql("stock = stock - " + quantity).setSql("sales = sales + " + quantity));
            if (updated == 0) {
                throw new BizException(ErrorCode.CONFLICT, "dish stock is insufficient");
            }
        }
    }

    private void releaseStock(List<OrderItem> items) {
        Map<Long, Integer> released = new HashMap<>();
        items.forEach(item -> released.merge(item.getDishId(), item.getQuantity(), Integer::sum));
        released.forEach((dishId, quantity) -> dishMapper.update(null, new LambdaUpdateWrapper<Dish>()
                .eq(Dish::getId, dishId).setSql("stock = stock + " + quantity)
                .setSql("sales = GREATEST(sales - " + quantity + ", 0)")));
    }

    private void settlePaymentOnCancellation(Long orderId, String reason) {
        // 模拟支付的退款恢复：成功记录标记为全额退款，未完成记录关闭并记录取消原因。
        List<PaymentRecord> payments = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId).orderByDesc(PaymentRecord::getCreatedAt));
        for (PaymentRecord payment : payments) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAmount(payment.getAmount());
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CLOSED);
                payment.setFailureReason(reason);
            } else {
                continue;
            }
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRecordMapper.updateById(payment);
        }
    }

    private List<CartItem> loadCartItems(long userId, List<Long> requestedIds) {
        List<CartItem> items;
        if (requestedIds == null || requestedIds.isEmpty()) {
            items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
                    .eq(CartItem::getStatus, CartItemStatus.ACTIVE).eq(CartItem::getSelected, true));
        } else {
            Set<Long> ids = new LinkedHashSet<>(requestedIds.stream().filter(Objects::nonNull).toList());
            items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().in(CartItem::getId, ids)
                    .eq(CartItem::getUserId, userId).eq(CartItem::getStatus, CartItemStatus.ACTIVE));
            if (items.stream().map(CartItem::getId).collect(java.util.stream.Collectors.toSet()).size() != ids.size()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "cart item does not exist or is not active");
            }
        }
        return items;
    }

    private DishSpec loadSpec(Long specId, Long dishId) {
        if (specId == null) return null;
        DishSpec spec = dishSpecMapper.selectById(specId);
        if (spec == null || !Objects.equals(spec.getDishId(), dishId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "dish spec does not match dish");
        }
        return spec;
    }

    private void markCartCheckedOut(List<CartItem> items) {
        LocalDateTime now = LocalDateTime.now();
        for (CartItem item : items) {
            int updated = cartItemMapper.update(null, new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getId, item.getId()).eq(CartItem::getStatus, CartItemStatus.ACTIVE)
                .set(CartItem::getStatus, CartItemStatus.CHECKED_OUT).set(CartItem::getUpdatedAt, now));
            if (updated == 0) {
                throw new BizException(ErrorCode.CONFLICT, "cart item changed concurrently");
            }
        }
    }

    private List<OrderItem> orderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    private OrderDto.Detail detail(Long id, Orders order, List<OrderItem> items) {
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        OrderDto.View view = view(order, merchant == null ? null : merchant.getName());
        return new OrderDto.Detail(view, items.stream().map(OrderService::itemView).toList());
    }

    private PageResponse<OrderDto.View> pageResponse(Page<Orders> result, int page, int size) {
        Map<Long, String> merchantNames = new HashMap<>();
        List<OrderDto.View> records = result.getRecords().stream().map(order -> {
            String name = merchantNames.computeIfAbsent(order.getMerchantId(), id -> {
                Merchant merchant = merchantMapper.selectById(id);
                return merchant == null ? null : merchant.getName();
            });
            return view(order, name);
        }).toList();
        return PageResponse.of(records, result.getTotal(), page, size);
    }

    private Orders requireVisibleOrder(Long id) {
        Orders order = requireOrder(id);
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("RIDER")
                || Objects.equals(order.getUserId(), SecurityUtils.requireCurrentUserId())) return order;
        return requireMerchantOrder(id);
    }

    private Orders requireOwnedOrder(Long id) {
        Orders order = requireOrder(id);
        if (!Objects.equals(order.getUserId(), SecurityUtils.requireCurrentUserId())) {
            throw new BizException(ErrorCode.ACCESS_DENIED);
        }
        return order;
    }

    private Orders requireMerchantOrder(Long id) {
        Orders order = requireOrder(id);
        if (SecurityUtils.hasRole("ADMIN")) return order;
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        if (merchant == null || !Objects.equals(merchant.getOwnerId(), SecurityUtils.requireCurrentUserId())) {
            throw new BizException(ErrorCode.ACCESS_DENIED);
        }
        return order;
    }

    private Orders requireDeliveryActor(Long id) {
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("RIDER")) {
            return requireOrder(id);
        }
        return requireMerchantOrder(id);
    }

    private void requireOwnedMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (!SecurityUtils.hasRole("ADMIN")
                && !Objects.equals(merchant.getOwnerId(), SecurityUtils.requireCurrentUserId())) {
            throw new BizException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Orders requireOrder(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order == null) throw new BizException(ErrorCode.NOT_FOUND);
        return order;
    }

    private static OrderDto.ItemView itemView(OrderItem item) {
        return new OrderDto.ItemView(item.getId(), item.getDishId(), item.getDishSpecId(), item.getDishName(),
                item.getSpecName(), item.getNote(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
    }

    private static OrderDto.View view(Orders order, String merchantName) {
        return new OrderDto.View(order.getId(), order.getOrderNo(), order.getUserId(), order.getMerchantId(), merchantName,
                order.getAddressId(), order.getCouponClaimId(), order.getStatus(), order.getTotalAmount(),
                order.getDeliveryFee(), order.getPackagingFee(), order.getDiscountAmount(), order.getPayableAmount(),
                order.getUserNote(), order.getMerchantNote(), order.getCancelReason(), order.getContactPhone(),
                order.getDeliveryAddressSnapshot(), order.getCreatedAt(), order.getAcceptedAt(), order.getReadyAt(),
                order.getPickedAt(), order.getDeliveredAt(), order.getCancelledAt(), order.getUpdatedAt());
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String addressSnapshot(UserAddress address) {
        return String.join(" ", address.getProvince(), address.getCity(),
                address.getDistrict() == null ? "" : address.getDistrict(), address.getDetail());
    }

    private static String newOrderNo() {
        return "SO" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }
}
