package com.mirror.product.enums;

/**
 * Status enum for POD QR Codes.
 */
public enum QrCodeStatus {
    /**
     * QR code is active and scannable
     */
    ACTIVE,

    /**
     * QR code is temporarily disabled
     */
    INACTIVE,

    /**
     * QR code has expired and is no longer valid
     */
    EXPIRED
}
