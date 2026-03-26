package com.mirror.product.enums;

/**
 * Status enum for Production Order Stages
 * Used to track the state of each stage in a production order workflow
 *
 * Status transitions:
 * - BLOCKED -> READY (when previous stage completes)
 * - READY -> IN_PROGRESS (when stage work begins)
 * - IN_PROGRESS -> COMPLETED (when stage work finishes)
 * - Any -> SKIPPED (when stage is intentionally bypassed)
 *
 * First stage in workflow starts as READY, all others start as BLOCKED
 */
public enum ProductionOrderStageStatus {
    BLOCKED,        // Waiting for previous stage(s) to complete
    READY,          // Ready to start work
    IN_PROGRESS,    // Work is actively being performed
    COMPLETED,      // Stage work has been completed
    SKIPPED         // Stage was intentionally skipped
}
