package com.example.delivery.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.Orders;
import com.example.delivery.domain.entity.PaymentRecord;
import com.example.delivery.domain.enums.PaymentMethod;
import com.example.delivery.domain.enums.PaymentStatus;
import com.example.delivery.domain.mapper.OrderMapper;
import com.example.delivery.domain.mapper.PaymentRecordMapper;
import com.example.delivery.modules.order.service.OrderService;
import com.example.delivery.modules.payment.dto.PaymentDto;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Deterministic mock payment used before a real payment gateway is integrated. */
@Service
public class PaymentService {
    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public PaymentService(PaymentRecordMapper paymentRecordMapper, OrderMapper orderMapper, OrderService orderService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentDto.View mock(PaymentDto.MockRequest request) {
        Orders order = requirePayableOrder(request.orderId());
        PaymentRecord existing = successfulPayment(order.getId());
        if (existing != null) {
            if (order.getStatus() == com.example.delivery.domain.enums.OrderStatus.PENDING_PAYMENT) {
                orderService.markPaid(order.getId());
            }
            return view(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        String transactionNo = request.transactionNo() == null || request.transactionNo().isBlank()
                ? "MOCK-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)
                : request.transactionNo().trim();
        PaymentRecord payment = PaymentRecord.builder().paymentNo(newPaymentNo()).orderId(order.getId())
                .userId(order.getUserId()).amount(nvl(order.getPayableAmount())).paymentMethod(request.paymentMethod())
                .status(PaymentStatus.SUCCESS).transactionNo(transactionNo).refundedAmount(BigDecimal.ZERO.setScale(2))
                .paidAt(now).createdAt(now).updatedAt(now).build();
        paymentRecordMapper.insert(payment);
        orderService.markPaid(order.getId());
        return view(payment);
    }

    @Transactional
    public PaymentDto.View mockPay(Long orderId, PaymentDto.MockPayRequest request) {
        return mock(new PaymentDto.MockRequest(orderId, PaymentMethod.MOCK_BALANCE,
                request == null ? null : request.transactionNo()));
    }

    @Transactional(readOnly = true)
    public PaymentDto.View get(Long paymentId) {
        PaymentRecord payment = paymentRecordMapper.selectById(paymentId);
        if (payment == null) throw new BizException(ErrorCode.NOT_FOUND);
        requireVisible(payment);
        return view(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto.View latestForOrder(Long orderId) {
        Orders order = requireVisibleOrder(orderId);
        List<PaymentRecord> payments = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, order.getId()).orderByDesc(PaymentRecord::getCreatedAt));
        return payments.isEmpty() ? null : view(payments.getFirst());
    }

    private Orders requirePayableOrder(Long orderId) {
        Orders order = requireOwnedOrder(orderId);
        if (order.getStatus() != com.example.delivery.domain.enums.OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ErrorCode.CONFLICT, "order is not waiting for payment");
        }
        return order;
    }

    private PaymentRecord successfulPayment(Long orderId) {
        return paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderId).eq(PaymentRecord::getStatus, PaymentStatus.SUCCESS)
                        .orderByDesc(PaymentRecord::getCreatedAt).last("LIMIT 1")).stream().findFirst().orElse(null);
    }

    private Orders requireOwnedOrder(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (!Objects.equals(order.getUserId(), SecurityUtils.requireCurrentUserId())) {
            throw new BizException(ErrorCode.ACCESS_DENIED);
        }
        return order;
    }

    private Orders requireVisibleOrder(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (SecurityUtils.hasRole("ADMIN") || Objects.equals(order.getUserId(), SecurityUtils.requireCurrentUserId())) {
            return order;
        }
        throw new BizException(ErrorCode.ACCESS_DENIED);
    }

    private void requireVisible(PaymentRecord payment) {
        if (!SecurityUtils.hasRole("ADMIN")
                && !Objects.equals(payment.getUserId(), SecurityUtils.requireCurrentUserId())) {
            throw new BizException(ErrorCode.ACCESS_DENIED);
        }
    }

    private static PaymentDto.View view(PaymentRecord payment) {
        return new PaymentDto.View(payment.getId(), payment.getPaymentNo(), payment.getOrderId(), payment.getUserId(),
                payment.getAmount(), payment.getPaymentMethod(), payment.getStatus(), payment.getTransactionNo(),
                payment.getFailureReason(), payment.getRefundedAmount(), payment.getPaidAt(), payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String newPaymentNo() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }
}
