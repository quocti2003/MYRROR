package com.mirror.product.dto.notification.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public class NotificationEvent {

    private final String type;
    private final String referenceId;
    private final Instant occurredAt;
    private final Map<String, Object> payload;

    @JsonCreator
    public NotificationEvent(
            @JsonProperty("type") String type,
            @JsonProperty("referenceId") String referenceId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("payload") Map<String, Object> payload) {
        this.type = type;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
