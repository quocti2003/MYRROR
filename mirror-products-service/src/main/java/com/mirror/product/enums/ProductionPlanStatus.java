package com.mirror.product.enums;

/**
 * Status enum for Production Plans
 * - DRAFT: Plan is being created/edited
 * - PLANNING: Plan is in planning phase, orders not yet created
 * - APPROVED: Plan is approved and ready for production
 * - IN_PRODUCTION: Production orders are actively being worked on
 * - COMPLETED: All production orders have been completed
 * - CANCELLED: Plan has been cancelled
 */
public enum ProductionPlanStatus {
    DRAFT,
    PLANNING,
    APPROVED,
    IN_PRODUCTION,
    COMPLETED,
    CANCELLED
}
