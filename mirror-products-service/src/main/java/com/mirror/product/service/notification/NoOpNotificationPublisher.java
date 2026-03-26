package com.mirror.product.service.notification;

import com.mirror.product.entity.Appointment;
import com.mirror.product.entity.Order;
import com.mirror.product.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of NotificationPublisher that does nothing.
 * This is used when notifications are disabled.
 * Activate by setting notification.service.mode=noop.
 */
@Service
@ConditionalOnProperty(prefix = "notification.service", name = "mode", havingValue = "noop")
public class NoOpNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpNotificationPublisher.class);

    @Override
    public void publishOrderCreated(Order order, String initiatedBy) {
        log.debug("Notification service disabled; skipping order created event for {}", order != null ? order.getId() : "unknown");
    }

    @Override
    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus, String changedBy, String note) {
        log.debug("Notification service disabled; skipping order status changed event for {}", order != null ? order.getId() : "unknown");
    }

    @Override
    public void publishAppointmentCreated(Appointment appointment, String venueName, String venueAddress) {
        log.debug("Notification service disabled; skipping appointment created event for {}", appointment != null ? appointment.getId() : "unknown");
    }
}
