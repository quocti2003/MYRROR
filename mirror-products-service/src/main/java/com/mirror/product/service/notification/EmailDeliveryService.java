package com.mirror.product.service.notification;

import com.mirror.product.config.notification.NotificationProperties;
import com.mirror.product.model.EmailEnvelope;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailDeliveryService(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(EmailEnvelope envelope) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            logSmtpConfiguration();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            // Set sender with display name if available
            String senderAddress = properties.getDefaultSender();
            String senderName = properties.getDefaultSenderName();
            if (senderName != null && !senderName.isBlank()) {
                helper.setFrom(senderAddress, senderName);
            } else {
                helper.setFrom(senderAddress);
            }

            helper.setTo(envelope.getRecipients().toArray(new String[0]));
            helper.setSubject(envelope.getSubject());
            helper.setText(envelope.getTextBody(), envelope.getHtmlBody());
            envelope.getAttachments().forEach((name, data) -> {
                try {
                    helper.addAttachment(name, new ByteArrayResource(data));
                } catch (MessagingException e) {
                    log.warn("Failed to attach file {}", name, e);
                }
            });
            mailSender.send(message);
            log.info("Email dispatched to {} with subject {}", envelope.getRecipients(), envelope.getSubject());
        } catch (Exception ex) {
            log.error("Failed to build email for recipients {}", envelope.getRecipients(), ex);
            throw new IllegalStateException("Failed to send email", ex);
        }
    }

    private void logSmtpConfiguration() {
        if (mailSender instanceof JavaMailSenderImpl sender) {
            var javaMailProps = sender.getJavaMailProperties();
            String senderInfo = properties.getDefaultSenderName() != null
                ? properties.getDefaultSenderName() + " <" + properties.getDefaultSender() + ">"
                : properties.getDefaultSender();
            log.info("SMTP config - host={}, port={}, username={}, auth={}, starttls={}, connectionTimeout={}, readTimeout={}, writeTimeout={}, defaultSender={}",
                    sender.getHost(),
                    sender.getPort(),
                    sender.getUsername(),
                    javaMailProps.getProperty("mail.smtp.auth"),
                    javaMailProps.getProperty("mail.smtp.starttls.enable"),
                    javaMailProps.getProperty("mail.smtp.connectiontimeout"),
                    javaMailProps.getProperty("mail.smtp.timeout"),
                    javaMailProps.getProperty("mail.smtp.writetimeout"),
                    senderInfo);
        } else {
            log.info("SMTP config logging skipped because mailSender is {}", mailSender.getClass().getName());
        }
    }
}
