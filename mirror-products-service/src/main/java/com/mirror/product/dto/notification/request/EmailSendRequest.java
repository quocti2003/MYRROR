package com.mirror.product.dto.notification.request;

import com.mirror.product.enums.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EmailSendRequest {

    @NotEmpty
    private Set<@Email String> recipients;

    private String subject;

    private String templateKey;

    private Map<String, Object> model;

    private String htmlBody;

    private String textBody;

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    private String captchaToken;

    public Set<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public EmailType getEmailType() {
        return emailType;
    }

    public void setEmailType(EmailType emailType) {
        this.emailType = emailType;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }

    public Set<String> safeRecipients() {
        if (recipients == null || recipients.isEmpty()) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(recipients);
    }

    public Map<String, Object> safeModel() {
        return model == null ? Collections.emptyMap() : model;
    }

    public String getSubjectOrDefault() {
        return (subject == null || subject.isBlank()) ? "Notification" : subject;
    }

    public String getTemplateKeyOrNull() {
        return (templateKey == null || templateKey.isBlank()) ? null : templateKey;
    }
}
