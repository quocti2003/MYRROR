package com.mirror.product.enums;

/**
 * Print method for sending jobs to printer
 */
public enum PrintMethod {
    NETWORK,    // TCP/IP raw printing to printer IP
    USB,        // Direct USB connection
    SPOOLER     // Windows print spooler
}
