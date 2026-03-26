package com.mirror.product.enums;

/**
 * Status enum for PODs (Point of Display units).
 * Represents the operational states of a POD.
 */
public enum PodStatus {
    /**
     * POD is being set up, not yet operational
     */
    DRAFT,

    /**
     * POD is active and operational
     */
    ACTIVE,

    /**
     * POD is under maintenance
     */
    MAINTENANCE,

    /**
     * POD is deactivated/closed
     */
    INACTIVE
}
