package com.mirror.product.service.notification;

import com.mirror.product.dto.notification.event.NotificationEvent;
import com.mirror.product.model.EmailEnvelope;
import com.mirror.product.util.RecipientExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final EmailTemplateRenderer templateRenderer;
    private final EmailDeliveryService deliveryService;

    public NotificationDispatcher(EmailTemplateRenderer templateRenderer, EmailDeliveryService deliveryService) {
        this.templateRenderer = templateRenderer;
        this.deliveryService = deliveryService;
    }

    public void dispatch(NotificationEvent event) {
        if (event == null) {
            log.warn("Received null event");
            return;
        }
        log.info("Processing notification event type={} reference={} occurredAt={} ",
                event.getType(), event.getReferenceId(), event.getOccurredAt());
        switch (event.getType()) {
            case "order.created" -> handleOrderCreated(event);
            case "order.status.changed" -> handleOrderStatusChanged(event);
            case "payment.due" -> handlePaymentDue(event);
            case "submission.thank.you" -> handleSubmissionThankYou(event);
            case "user.email.verification" -> handleEmailVerification(event);
            case "user.password.reset" -> handlePasswordReset(event);
            default -> log.warn("Unhandled notification type: {}", event.getType());
        }
    }

    private void handleOrderCreated(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        Set<String> recipients = RecipientExtractor.resolveRecipients(payload);
        if (recipients.isEmpty()) {
            log.warn("No recipients found for order.created event {}", event.getReferenceId());
            return;
        }
        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject("Order created: " + event.getReferenceId())
                .htmlBody(templateRenderer.renderHtml("order.created", payload))
                .textBody(templateRenderer.buildFallbackBody(payload))
                .build();
        deliveryService.send(envelope);
    }

    private void handleOrderStatusChanged(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        Set<String> recipients = RecipientExtractor.resolveRecipients(payload);
        if (recipients.isEmpty()) {
            log.warn("No recipients found for order.status.changed event {}", event.getReferenceId());
            return;
        }
        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject("Order status update: " + payload.getOrDefault("status", "unknown"))
                .htmlBody(templateRenderer.renderHtml("order.status.changed", payload))
                .textBody(templateRenderer.buildFallbackBody(payload))
                .build();
        deliveryService.send(envelope);
    }

    private void handlePaymentDue(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        Set<String> recipients = RecipientExtractor.resolveRecipients(payload);
        if (recipients.isEmpty()) {
            log.warn("No recipients found for payment.due event {}", event.getReferenceId());
            return;
        }
        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject("Payment reminder for order " + event.getReferenceId())
                .htmlBody(templateRenderer.renderHtml("payment.due", payload))
                .textBody(templateRenderer.buildFallbackBody(payload))
                .build();
        deliveryService.send(envelope);
    }

    private void handleSubmissionThankYou(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        Set<String> recipients = RecipientExtractor.resolveRecipients(payload);
        if (recipients.isEmpty()) {
            log.warn("No recipients found for submission.thank.you event {}", event.getReferenceId());
            return;
        }
        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject("Thank You for Your Submission - \"Reflections of Tomorrow - Be a Creative Partner with Mirror\"")
                .htmlBody(templateRenderer.renderHtml("submission.thank.you", payload))
                .textBody(templateRenderer.buildFallbackBody(payload))
                .build();
        deliveryService.send(envelope);
    }

    private void handleEmailVerification(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        Set<String> recipients = RecipientExtractor.resolveRecipients(payload);
        if (recipients.isEmpty()) {
            log.warn("No recipients found for user.email.verification event {}", event.getReferenceId());
            return;
        }

        String username = (String) payload.getOrDefault("username", "User");
        String verificationUrl = (String) payload.get("verificationUrl");

        if (verificationUrl == null || verificationUrl.isBlank()) {
            log.error("Missing verificationUrl for email verification event {}", event.getReferenceId());
            return;
        }

        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject("Verify Your Email Address - Mirror")
                .htmlBody(templateRenderer.renderHtml("user.email.verification", payload))
                .textBody(templateRenderer.buildFallbackBody(payload))
                .build();
        deliveryService.send(envelope);
    }

    private void handlePasswordReset(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        Set<String> recipients = RecipientExtractor.resolveRecipients(payload);
        if (recipients.isEmpty()) {
            log.warn("No recipients found for user.password.reset event {}", event.getReferenceId());
            return;
        }

        String username = (String) payload.getOrDefault("username", "User");
        String otp = (String) payload.get("otp");

        if (otp == null || otp.isBlank()) {
            log.error("Missing OTP for password reset event {}", event.getReferenceId());
            return;
        }

        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject("Password Reset Request - Mirror")
                .htmlBody(templateRenderer.renderHtml("user.password.reset", payload))
                .textBody(templateRenderer.buildFallbackBody(payload))
                .build();
        deliveryService.send(envelope);
    }
}
