package com.mirror.product.enums;

/**
 * Status enum for POD Attributions.
 */
public enum AttributionStatus {
    /**
     * Attribution pending confirmation
     */
    PENDING,

    /**
     * Attribution confirmed
     */
    CONFIRMED,

    /**
     * Attribution cancelled (e.g., order refunded)
     */
    CANCELLED
}
