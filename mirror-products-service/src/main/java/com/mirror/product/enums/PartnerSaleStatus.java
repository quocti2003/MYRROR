package com.mirror.product.enums;

/**
 * Status enum for Partner Sales.
 * Represents the lifecycle of a sale made by a phygital partner.
 */
public enum PartnerSaleStatus {
    /**
     * Sale pending confirmation
     */
    PENDING,

    /**
     * Sale confirmed
     */
    CONFIRMED,

    /**
     * Sale completed (delivered to customer)
     */
    COMPLETED,

    /**
     * Sale cancelled
     */
    CANCELLED,

    /**
     * Goods returned by customer
     */
    RETURNED
}
