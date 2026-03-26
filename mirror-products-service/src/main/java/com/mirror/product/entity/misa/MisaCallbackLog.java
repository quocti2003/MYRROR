package com.mirror.product.entity.misa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to log all MISA callback events for auditing and debugging
 */
@Entity
@Table(name = "misa_callback_logs", indexes = {
    @Index(name = "idx_callback_data_type", columnList = "data_type"),
    @Index(name = "idx_callback_org_ref_id", columnList = "org_ref_id"),
    @Index(name = "idx_callback_received_at", columnList = "received_at"),
    @Index(name = "idx_callback_processed", columnList = "processed")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisaCallbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * App ID from the callback request
     */
    @Column(name = "app_id", nullable = false)
    private String appId;

    /**
     * Organization company code
     */
    @Column(name = "org_company_code", nullable = false)
    private String orgCompanyCode;

    /**
     * Callback data type:
     * 1=Save, 2=Delete, 3=Update, 6=Export, 7=Dictionary, 8=PaymentRequest, 15=PaymentResponse
     */
    @Column(name = "data_type", nullable = false)
    private Integer dataType;

    /**
     * Human readable data type name
     */
    @Column(name = "data_type_name")
    private String dataTypeName;

    /**
     * Original reference ID from our system
     */
    @Column(name = "org_ref_id")
    private String orgRefId;

    /**
     * Original reference number
     */
    @Column(name = "org_ref_no")
    private String orgRefNo;

    /**
     * MISA's internal reference ID
     */
    @Column(name = "misa_ref_id")
    private String misaRefId;

    /**
     * MISA's internal reference number
     */
    @Column(name = "misa_ref_no")
    private String misaRefNo;

    /**
     * Voucher type if applicable
     */
    @Column(name = "voucher_type")
    private Integer voucherType;

    /**
     * Whether the MISA operation was successful
     */
    @Column(name = "misa_success", nullable = false)
    private Boolean misaSuccess;

    /**
     * Error code from MISA if failed
     */
    @Column(name = "misa_error_code")
    private String misaErrorCode;

    /**
     * Error message from MISA if failed
     */
    @Column(name = "misa_error_message", columnDefinition = "TEXT")
    private String misaErrorMessage;

    /**
     * Whether our system processed this callback
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    /**
     * Our processing result
     */
    @Column(name = "processing_success")
    private Boolean processingSuccess;

    /**
     * Our processing error message if failed
     */
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    /**
     * Raw JSON payload for debugging
     */
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    /**
     * Signature from the request
     */
    @Column
    private String signature;

    /**
     * Whether signature verification passed
     */
    @Column(name = "signature_valid")
    private Boolean signatureValid;

    /**
     * When the callback was received
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * When processing completed
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Convert data type to human readable name
     */
    public static String getDataTypeName(Integer dataType) {
        if (dataType == null) return "UNKNOWN";
        return switch (dataType) {
            case 1 -> "VOUCHER_SAVE";
            case 2 -> "VOUCHER_DELETE";
            case 3 -> "VOUCHER_UPDATE";
            case 6 -> "WAREHOUSE_EXPORT";
            case 7 -> "DICTIONARY_CREATE";
            case 8 -> "PAYMENT_REQUEST";
            case 15 -> "PAYMENT_RESPONSE";
            default -> "UNKNOWN_" + dataType;
        };
    }

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (dataType != null && dataTypeName == null) {
            dataTypeName = getDataTypeName(dataType);
        }
    }
}
