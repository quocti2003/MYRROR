package com.mirror.product.service.notification;

import com.mirror.product.config.NotificationServiceProperties;
import com.mirror.product.entity.Appointment;
import com.mirror.product.entity.Order;
import com.mirror.product.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * REST-based implementation of NotificationPublisher that makes HTTP requests
 * to an external notification service. This is kept for backward compatibility
 * and can be enabled by setting notification.service.mode=rest.
 */
@Service
@ConditionalOnProperty(prefix = "notification.service", name = "mode", havingValue = "rest")
public class RestNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestNotificationPublisher.class);

    private final RestClient restClient;
    private final NotificationServiceProperties properties;

    public RestNotificationPublisher(RestClient restClient, NotificationServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
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
        sendEvent("order.created", order.getId(), payload);
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
        sendEvent("order.status.changed", order.getId(), payload);
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

        Map<String, Object> body = new HashMap<>();
        body.put("customerTitle", appointment.getCustomerTitle() != null ? appointment.getCustomerTitle() : "");
        body.put("customerName", appointment.getCustomerFirstName() + " " + appointment.getCustomerLastName());
        body.put("customerEmail", appointment.getCustomerEmail());
        body.put("service", appointment.getService() != null ? appointment.getService() : "In-store appointment");
        body.put("venueName", venueName);
        body.put("venueAddress", venueAddress);
        body.put("appointmentDate", formattedDate);
        body.put("appointmentTime", formattedTime);

        try {
            RestClient.RequestHeadersSpec<?> request = restClient
                    .post()
                    .uri("/internal/notifications/appointment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body);

            if (StringUtils.hasText(properties.getApiKey())) {
                request = request.header("X-API-Key", properties.getApiKey());
            }

            request.retrieve()
                    .toBodilessEntity();

            log.info("Appointment confirmation email sent for appointment {}", appointment.getId());
        } catch (Exception ex) {
            log.warn("Failed to send appointment confirmation email for appointment {}", appointment.getId(), ex);
        }
    }

    private void sendEvent(String type, String referenceId, Map<String, Object> payload) {
        Map<String, Object> body = Map.of(
                "type", type,
                "referenceId", referenceId,
                "occurredAt", Instant.now(),
                "payload", payload
        );

        try {
            RestClient.RequestHeadersSpec<?> request = restClient
                    .post()
                    .uri("/internal/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body);

            if (StringUtils.hasText(properties.getApiKey())) {
                request = request.header("X-API-Key", properties.getApiKey());
            }

            request.retrieve()
                    .toBodilessEntity();

            log.debug("Notification event {} sent for order {}", type, referenceId);
        } catch (Exception ex) {
            log.warn("Failed to send notification event {} for order {}", type, referenceId, ex);
        }
    }
}
