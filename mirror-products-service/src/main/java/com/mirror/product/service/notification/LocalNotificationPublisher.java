package com.mirror.product.service.notification;

import com.mirror.product.dto.notification.event.NotificationEvent;
import com.mirror.product.dto.notification.request.EmailSendRequest;
import com.mirror.product.entity.Appointment;
import com.mirror.product.entity.Order;
import com.mirror.product.enums.EmailType;
import com.mirror.product.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Local implementation of NotificationPublisher that directly calls
 * the notification services instead of making HTTP requests.
 * This is the default (primary) implementation for the monolith architecture.
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "notification.service", name = "mode", havingValue = "local", matchIfMissing = true)
public class LocalNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationPublisher.class);

    private final NotificationDispatcher dispatcher;
    private final DirectEmailService directEmailService;

    public LocalNotificationPublisher(NotificationDispatcher dispatcher, DirectEmailService directEmailService) {
        this.dispatcher = dispatcher;
        this.directEmailService = directEmailService;
    }

    @Override
    public void publishOrderCreated(Order order, String initiatedBy) {
        if (!isEligible(order)) {
            return;
        }

        Map<String, Object> payload = defaultOrderPayload(order);
        payload.put("recipients", List.of(order.getCustomerEmail().trim()));
        payload.put("initiatedBy", initiatedBy);
        payload.put("status", order.getStatus());
        payload.put("notes", order.getNotes());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("placedAt", order.getPlacedAt());

        NotificationEvent event = new NotificationEvent(
                "order.created",
                order.getId(),
                Instant.now(),
                payload
        );

        try {
            dispatcher.dispatch(event);
            log.debug("Order created notification dispatched for order {}", order.getId());
        } catch (Exception ex) {
            log.warn("Failed to dispatch order created notification for order {}", order.getId(), ex);
        }
    }

    @Override
    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus, String changedBy, String note) {
        if (!isEligible(order)) {
            return;
        }

        Map<String, Object> payload = defaultOrderPayload(order);
        payload.put("recipients", List.of(order.getCustomerEmail().trim()));
        payload.put("status", order.getStatus());
        payload.put("previousStatus", previousStatus);
        payload.put("note", note);
        payload.put("changedBy", changedBy);
        payload.put("lastStatusUpdatedAt", order.getLastStatusUpdatedAt());

        NotificationEvent event = new NotificationEvent(
                "order.status.changed",
                order.getId(),
                Instant.now(),
                payload
        );

        try {
            dispatcher.dispatch(event);
            log.debug("Order status changed notification dispatched for order {}", order.getId());
        } catch (Exception ex) {
            log.warn("Failed to dispatch order status changed notification for order {}", order.getId(), ex);
        }
    }

    @Override
    public void publishAppointmentCreated(Appointment appointment, String venueName, String venueAddress) {
        if (appointment == null) {
            return;
        }
        if (!StringUtils.hasText(appointment.getCustomerEmail())) {
            log.debug("Skipping appointment notification for {} because customer email is missing", appointment.getId());
            return;
        }

        // Format date and time for display
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

        String formattedDate = appointment.getAppointmentDate().format(dateFormatter);
        String formattedTime = appointment.getAppointmentTime().format(timeFormatter);

        Map<String, Object> model = new HashMap<>();
        model.put("customerTitle", appointment.getCustomerTitle() != null ? appointment.getCustomerTitle() : "");
        model.put("customerName", appointment.getCustomerFirstName() + " " + appointment.getCustomerLastName());
        model.put("customerEmail", appointment.getCustomerEmail());
        model.put("service", appointment.getService() != null ? appointment.getService() : "In-store appointment");
        model.put("venueName", venueName);
        model.put("venueAddress", venueAddress);
        model.put("appointmentDate", formattedDate);
        model.put("appointmentTime", formattedTime);

        EmailSendRequest emailRequest = new EmailSendRequest();
        emailRequest.setRecipients(Set.of(appointment.getCustomerEmail()));
        emailRequest.setEmailType(EmailType.APPOINTMENT_CONFIRMATION);
        emailRequest.setModel(model);

        try {
            directEmailService.send(emailRequest);
            log.info("Appointment confirmation email sent for appointment {}", appointment.getId());
        } catch (Exception ex) {
            log.warn("Failed to send appointment confirmation email for appointment {}", appointment.getId(), ex);
        }
    }

    private boolean isEligible(Order order) {
        if (order == null) {
            return false;
        }
        if (!StringUtils.hasText(order.getCustomerEmail())) {
            log.debug("Skipping notification for order {} because customer email is missing", order.getId());
            return false;
        }
        return true;
    }

    private Map<String, Object> defaultOrderPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("customerName", order.getCustomerName());
        payload.put("customerEmail", order.getCustomerEmail());
        payload.put("customerPhone", order.getCustomerPhone());
        payload.put("userId", order.getUserId());
        payload.put("productId", order.getProductId());
        payload.put("paymentStatus", order.getPaymentStatus());
        payload.put("paymentOutstanding", order.getPaymentOutstanding());
        payload.put("expectedDeliveryDate", order.getExpectedDeliveryDate());
        return payload;
    }
}
