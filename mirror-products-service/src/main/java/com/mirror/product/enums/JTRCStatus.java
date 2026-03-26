package com.mirror.product.enums;

/**
 * Status enum for JTRC (Jewelry Technical Report Card) lifecycle
 */
public enum JTRCStatus {
    DRAFT,      // Initial state, can be edited
    APPROVED,   // Approved for production, locked from major edits
    ARCHIVED    // Historical record, no longer active
}
