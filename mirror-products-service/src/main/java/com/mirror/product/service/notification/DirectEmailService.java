package com.mirror.product.service.notification;

import com.mirror.product.dto.notification.request.EmailSendRequest;
import com.mirror.product.model.EmailEnvelope;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class DirectEmailService {

    private static final Logger log = LoggerFactory.getLogger(DirectEmailService.class);

    private final EmailTemplateRenderer templateRenderer;
    private final EmailDeliveryService deliveryService;

    public DirectEmailService(EmailTemplateRenderer templateRenderer, EmailDeliveryService deliveryService) {
        this.templateRenderer = templateRenderer;
        this.deliveryService = deliveryService;
    }

    public void send(@Valid EmailSendRequest request) {
        Set<String> recipients = request.safeRecipients();
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }

        Map<String, Object> model = request.safeModel();

        // Use emailType to determine template and subject if not explicitly provided
        String templateKey = request.getTemplateKeyOrNull();
        String subject = request.getSubject();

        if (request.getEmailType() != null) {
            if (templateKey == null) {
                templateKey = request.getEmailType().getTemplateKey();
            }
            if (subject == null || subject.isBlank()) {
                subject = request.getEmailType().getSubject();
            }
        }

        String htmlBody = request.getHtmlBody();
        if (htmlBody == null && templateKey != null) {
            htmlBody = templateRenderer.renderHtml(templateKey, model);
        }

        String textBody = request.getTextBody();
        if (textBody == null) {
            textBody = templateRenderer.buildFallbackBody(model);
        }

        EmailEnvelope envelope = EmailEnvelope.builder()
                .recipients(recipients)
                .subject(subject != null ? subject : "Notification")
                .htmlBody(htmlBody)
                .textBody(textBody)
                .build();

        log.debug("Dispatching direct email to {} using type {} and template {}",
                recipients, request.getEmailType(), templateKey);
        deliveryService.send(envelope);
    }
}
