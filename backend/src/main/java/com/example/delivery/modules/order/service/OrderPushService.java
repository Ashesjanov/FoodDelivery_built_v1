package com.example.delivery.modules.order.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * 订单 WebSocket 推送服务：把订单状态事件发送到 STOMP 主题 /topic/orders/{orderId}。
 * 依赖 {@link SimpMessagingTemplate}；在活动事务中仅注册 afterCommit 回调，
 * 保证订阅端只收到已经提交成功的状态，无事务时立即发布。
 */
@Component
public class OrderPushService {
    private final SimpMessagingTemplate messagingTemplate;

    public OrderPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** 将事件写入订单专属主题；由订单、支付和配送服务的状态变更链路统一调用。 */
    public void publishAfterCommit(Long orderId, String status, String message) {
        Runnable publish = () -> messagingTemplate.convertAndSend("/topic/orders/" + orderId,
                new OrderEvent(orderId, status, message, Instant.now()));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    /** 发往 /topic/orders/{orderId} 的订单状态事件载荷。 */
    public record OrderEvent(Long orderId, String status, String message, Instant timestamp) {
    }
}
