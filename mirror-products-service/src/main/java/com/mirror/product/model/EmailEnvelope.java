package com.mirror.product.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class EmailEnvelope {

    private final Set<String> recipients;
    private final String subject;
    private final String htmlBody;
    private final String textBody;
    private final Map<String, byte[]> attachments;

    private EmailEnvelope(Builder builder) {
        this.recipients = builder.recipients;
        this.subject = builder.subject;
        this.htmlBody = builder.htmlBody;
        this.textBody = builder.textBody;
        this.attachments = builder.attachments;
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public Map<String, byte[]> getAttachments() {
        return attachments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Set<String> recipients = Collections.emptySet();
        private String subject;
        private String htmlBody;
        private String textBody;
        private Map<String, byte[]> attachments = Collections.emptyMap();

        public Builder recipients(Set<String> recipients) {
            this.recipients = recipients;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        public Builder textBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        public Builder attachments(Map<String, byte[]> attachments) {
            this.attachments = attachments;
            return this;
        }

        public EmailEnvelope build() {
            return new EmailEnvelope(this);
        }
    }
}
