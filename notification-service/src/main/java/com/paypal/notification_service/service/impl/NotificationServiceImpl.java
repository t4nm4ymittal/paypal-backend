package com.paypal.notification_service.service.impl;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.repository.NotificationRepository;
import com.paypal.notification_service.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification sendNotification(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        if (notification.getStatus() == null) {
            notification.setStatus("PENDING");
        }
        return notificationRepository.save(notification);
    }

    @Override
    public Optional<Notification> getNotification(Long id) {
        return notificationRepository.findById(id);
    }

    @Override
    public List<Notification> getNotificationsBySender(Long senderAccountId) {
        return notificationRepository.findBySenderAccountId(senderAccountId);
    }

    @Override
    public List<Notification> getNotificationsByReceiver(Long receiverAccountId) {
        return notificationRepository.findByReceiverAccountId(receiverAccountId);
    }
}

