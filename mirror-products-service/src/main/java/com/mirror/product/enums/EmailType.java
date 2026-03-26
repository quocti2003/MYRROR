package com.mirror.product.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EmailType {
    MILAN_FORM("milan-form", "submission.thank.you", "Thank You for Your Submission - \"Reflections of Tomorrow - Be a Creative Partner with Mirror\""),
    EMAIL_VERIFICATION("email-verification", "email-verification", "Verify Your Email Address - Mirror"),
    APPOINTMENT_CONFIRMATION("appointment-confirmation", "appointment-confirmation", "Your Appointment is Confirmed - Mirror");

    private final String value;
    private final String templateKey;
    private final String subject;

    EmailType(String value, String templateKey, String subject) {
        this.value = value;
        this.templateKey = templateKey;
        this.subject = subject;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getSubject() {
        return subject;
    }

    @JsonCreator
    public static EmailType fromValue(String value) {
        for (EmailType type : EmailType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid email type: " + value + ". Available types: " + getAvailableTypes());
    }

    private static String getAvailableTypes() {
        StringBuilder sb = new StringBuilder();
        for (EmailType type : EmailType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(type.value);
        }
        return sb.toString();
    }
}
