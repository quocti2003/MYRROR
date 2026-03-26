package com.mirror.product.enums;

/**
 * Status of a print job
 */
public enum PrintJobStatus {
    PENDING,    // Job created, waiting to start
    PRINTING,   // Job is currently printing
    COMPLETED,  // Job completed successfully
    FAILED,     // Job failed with errors
    CANCELLED   // Job was cancelled by user
}
