package com.example.delivery.modules.order.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/** Publishes committed order changes to the order-specific STOMP topic. */
@Component
public class OrderPushService {
    private final SimpMessagingTemplate messagingTemplate;

    public OrderPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

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

    public record OrderEvent(Long orderId, String status, String message, Instant timestamp) {
    }
}
