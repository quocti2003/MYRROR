package com.mirror.product.enums;

/**
 * Type enum for POD Partners.
 * Distinguishes between traditional location partners and phygital franchise partners.
 */
public enum PartnerType {
    /**
     * Traditional POD partner - commission model.
     * Mirror owns inventory, partner earns commission on sales.
     */
    LOCATION,

    /**
     * Phygital franchise partner - wholesale/margin model.
     * Partner purchases inventory at wholesale price and resells.
     */
    PHYGITAL
}
