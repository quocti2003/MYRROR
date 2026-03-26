package com.mirror.product.service.notification;

import com.mirror.product.entity.Appointment;
import com.mirror.product.entity.Order;
import com.mirror.product.enums.OrderStatus;

public interface NotificationPublisher {

    void publishOrderCreated(Order order, String initiatedBy);

    void publishOrderStatusChanged(Order order, OrderStatus previousStatus, String changedBy, String note);

    void publishAppointmentCreated(Appointment appointment, String venueName, String venueAddress);
}
