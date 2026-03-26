package com.mirror.product.enums;

/**
 * Status enum for Production Orders
 * Used to track the overall state of a production order
 *
 * Status workflow:
 * - DRAFT: Order created but not yet started
 * - READY: Order is ready to begin production
 * - IN_PROGRESS: At least one stage is being worked on
 * - COMPLETED: All stages completed successfully
 * - CANCELLED: Order was cancelled before completion
 */
public enum ProductionOrderStatus {
    DRAFT,          // Order created, not yet started
    READY,          // Order ready to begin production
    IN_PROGRESS,    // Production is actively underway
    COMPLETED,      // All stages completed successfully
    CANCELLED       // Order was cancelled
}
