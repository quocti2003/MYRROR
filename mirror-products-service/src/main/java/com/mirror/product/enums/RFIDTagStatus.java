package com.mirror.product.enums;

/**
 * Status of an RFID tag
 */
public enum RFIDTagStatus {
    ACTIVE,     // Tag is active and valid
    INACTIVE,   // Tag is temporarily disabled
    VOIDED      // Tag has been voided (lost, damaged, etc.)
}
