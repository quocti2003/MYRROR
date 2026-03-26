package com.mirror.product.enums;

/**
 * Status enum for POD Commissions.
 * Represents the payment lifecycle of partner commissions.
 */
public enum CommissionStatus {
    /**
     * Commission calculated but not yet approved
     */
    PENDING,

    /**
     * Commission approved for payment
     */
    APPROVED,

    /**
     * Commission has been paid to partner
     */
    PAID,

    /**
     * Commission was cancelled (e.g., due to order cancellation)
     */
    CANCELLED
}
