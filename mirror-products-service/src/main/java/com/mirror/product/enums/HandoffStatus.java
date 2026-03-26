package com.mirror.product.enums;

/**
 * Status of a component handoff/transfer
 */
public enum HandoffStatus {
    /**
     * Handoff has been initiated, waiting for physical transfer
     */
    INITIATED,

    /**
     * Component is in transit between locations
     */
    IN_TRANSIT,

    /**
     * Receiving party has confirmed receipt of component
     */
    RECEIVED,

    /**
     * Receiving party has rejected the handoff (quality issue, wrong item, etc.)
     */
    REJECTED,

    /**
     * Handoff was cancelled before completion
     */
    CANCELLED
}
