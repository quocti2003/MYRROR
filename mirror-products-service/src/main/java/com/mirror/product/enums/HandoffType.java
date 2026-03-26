package com.mirror.product.enums;

/**
 * Type of component handoff/transfer event
 */
public enum HandoffType {
    /**
     * Initial assignment of component to first vendor in workflow
     */
    INITIAL_ASSIGNMENT,

    /**
     * Vendor starts working on their stage (receives component)
     */
    STAGE_START,

    /**
     * Vendor completes their stage (ready to hand off)
     */
    STAGE_COMPLETE,

    /**
     * Vendor reassignment - component transferred to different vendor
     */
    VENDOR_REASSIGN,

    /**
     * Final delivery back to MIRROR (last stage completion)
     */
    RETURN_TO_MIRROR,

    /**
     * Component returned to vendor for rework
     */
    REWORK_RETURN
}
