package com.mirror.product.enums;

/**
 * Status enum for Workflow Templates
 * - DRAFT: Template is being created/edited
 * - ACTIVE: Template is available for use in production plans
 * - ARCHIVED: Template is no longer active but preserved for history
 */
public enum WorkflowTemplateStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
