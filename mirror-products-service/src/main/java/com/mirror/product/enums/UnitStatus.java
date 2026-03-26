package com.mirror.product.enums;

/**
 * Status enum for individual product units in serialized inventory.
 */
public enum UnitStatus {
    AVAILABLE,      // In stock, ready for sale
    RESERVED,       // Reserved for a customer/order
    SOLD,           // Sold to customer
    IN_REPAIR,      // Sent for repair/maintenance
    RETURNED,       // Returned by customer
    DAMAGED,        // Damaged, not sellable
    TRANSFERRED,    // Transferred to another location
    CONSIGNMENT     // On consignment at a partner
}
