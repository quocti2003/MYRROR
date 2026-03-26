package com.mirror.product.enums;

/**
 * Attribution type enum for POD order attribution.
 * Defines how credit is assigned for an order.
 */
public enum AttributionType {
    /**
     * Full credit to the first touch point
     */
    FIRST_TOUCH,

    /**
     * Full credit to the last touch point before conversion
     */
    LAST_TOUCH,

    /**
     * Credit distributed across multiple touch points
     */
    MULTI_TOUCH
}
