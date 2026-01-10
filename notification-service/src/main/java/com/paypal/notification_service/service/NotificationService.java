package com.paypal.notification_service.service;

import com.paypal.notification_service.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationService {

    Notification sendNotification(Notification notification);

    Optional<Notification> getNotification(Long id);

    List<Notification> getNotificationsBySender(Long senderAccountId);

    List<Notification> getNotificationsByReceiver(Long receiverAccountId);
}

