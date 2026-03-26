package com.mirror.product.enums;

/**
 * Status of a print job item (individual label)
 */
public enum PrintJobItemStatus {
    PENDING,    // Item waiting to be printed
    PRINTED,    // Item printed successfully
    FAILED      // Item failed to print
}
