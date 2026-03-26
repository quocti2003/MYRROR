package com.mirror.product.enums;

/**
 * Type enum for POD QR Codes.
 * Different QR code types serve different purposes in the POD system.
 */
public enum QrCodeType {
    /**
     * General POD QR - links to POD page
     */
    POD_QR,

    /**
     * Specific product QR - links to product detail
     */
    PIECE_QR,

    /**
     * Partner-level QR - links to partner's catalog
     */
    PARTNER_QR,

    /**
     * AR/VR experience QR
     */
    EXPERIENCE_QR,

    /**
     * Event-specific QR (temporary)
     */
    EVENT_QR,

    /**
     * Phygital partner's product QR (leads to purchase from partner inventory)
     */
    PHYGITAL_QR
}
