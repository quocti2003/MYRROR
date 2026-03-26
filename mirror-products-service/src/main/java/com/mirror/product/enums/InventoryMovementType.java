package com.mirror.product.enums;

/**
 * Type enum for Inventory Movements.
 * Tracks all stock changes in a phygital partner's inventory.
 */
public enum InventoryMovementType {
    /**
     * Stock received from wholesale order
     */
    WHOLESALE_IN,

    /**
     * Stock sold to customer
     */
    SALE_OUT,

    /**
     * Manual stock adjustment (increase)
     */
    ADJUSTMENT_IN,

    /**
     * Manual stock adjustment (decrease)
     */
    ADJUSTMENT_OUT,

    /**
     * Customer return received
     */
    RETURN_IN,

    /**
     * Stock transferred out
     */
    TRANSFER_OUT,

    /**
     * Damaged/lost stock
     */
    DAMAGED_OUT
}
