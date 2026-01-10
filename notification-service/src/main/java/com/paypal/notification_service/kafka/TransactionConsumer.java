package com.paypal.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.entity.TransactionRef;
import com.paypal.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public TransactionConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transactions", groupId = "notification-service-group")
    public void listen(String message) {
        try {
            // Deserialize into TransactionRef (lightweight DTO) and map to Notification
            TransactionRef tx = objectMapper.readValue(message, TransactionRef.class);

            Notification notification = new Notification();
            notification.setTransactionId(tx.getId());
            notification.setSenderAccountId(tx.getSenderAccountId());
            notification.setReceiverAccountId(tx.getReceiverAccountId());
            notification.setAmount(tx.getAmount());
            notification.setCurrency(tx.getCurrency());
            // map transaction.description -> notification.message
            notification.setMessage(tx.getDescription());
            notification.setCreatedAt(tx.getCreatedAt());
            notification.setStatus(tx.getStatus());

            notificationService.sendNotification(notification);
            logger.info("Saved notification for transaction: {}", notification.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to process transaction message: {}", message, e);
        }
    }
}
