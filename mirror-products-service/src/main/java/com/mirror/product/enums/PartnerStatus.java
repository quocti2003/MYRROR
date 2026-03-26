package com.mirror.product.enums;

/**
 * Status enum for POD Partners.
 * Represents the lifecycle states of a partner in the PODs system.
 */
public enum PartnerStatus {
    /**
     * Partner has applied but not yet reviewed
     */
    PENDING,

    /**
     * Partner application approved, awaiting activation
     */
    APPROVED,

    /**
     * Partner is active and can operate PODs
     */
    ACTIVE,

    /**
     * Partner temporarily suspended
     */
    SUSPENDED,

    /**
     * Partner relationship terminated
     */
    TERMINATED
}
