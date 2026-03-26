package com.mirror.product.enums;

/**
 * Status enum for Wholesale Orders.
 * Represents the lifecycle of a wholesale purchase order from a phygital partner.
 */
public enum WholesaleOrderStatus {
    /**
     * Partner is drafting the order
     */
    DRAFT,

    /**
     * Order submitted for Mirror approval
     */
    SUBMITTED,

    /**
     * Mirror has approved the order
     */
    APPROVED,

    /**
     * Order is being processed/packaged
     */
    PROCESSING,

    /**
     * Order has been shipped
     */
    SHIPPED,

    /**
     * Order delivered to partner
     */
    DELIVERED,

    /**
     * Order completed, inventory received
     */
    COMPLETED,

    /**
     * Order cancelled
     */
    CANCELLED,

    /**
     * Goods returned
     */
    RETURNED
}
