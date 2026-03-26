package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MISA AMIS Open API Callback Request DTO
 *
 * This is the structure MISA sends to our callback endpoint after async operations complete.
 * Based on: https://actdocs.misa.vn/g2/graph/ACTOpenAPIHelp/index.html#6-1
 *
 * Data Types:
 * - 1: Save operations (voucher created)
 * - 2: Delete operations (voucher deleted)
 * - 3: Modification operations (voucher updated)
 * - 6: Warehouse export synchronization
 * - 7: Dictionary creation (master data created)
 * - 8: Payment status request
 * - 15: Payment status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisaCallbackRequest {

    /**
     * Application ID provided by MISA during registration
     */
    @JsonProperty("app_id")
    private String appId;

    /**
     * Customer domain code for data differentiation
     */
    @JsonProperty("org_company_code")
    private String orgCompanyCode;

    /**
     * SHA256HMAC signature for request verification
     * Key is the app_id
     */
    @JsonProperty("signature")
    private String signature;

    /**
     * Type of callback data
     * 1=Save, 2=Delete, 3=Update, 6=Export, 7=Dictionary, 8=PaymentRequest, 15=PaymentResponse
     */
    @JsonProperty("data_type")
    private Integer dataType;

    /**
     * The actual callback payload data
     */
    @JsonProperty("data")
    private CallbackData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackData {

        /**
         * Overall success status
         */
        @JsonProperty("success")
        private Boolean success;

        /**
         * Error code if failed
         */
        @JsonProperty("error_code")
        private String errorCode;

        /**
         * Error message description
         */
        @JsonProperty("error_message")
        private String errorMessage;

        /**
         * Original reference ID from our system
         */
        @JsonProperty("org_refid")
        private String orgRefId;

        /**
         * Original reference number from our system
         */
        @JsonProperty("org_refno")
        private String orgRefNo;

        /**
         * MISA's internal reference ID
         */
        @JsonProperty("ref_id")
        private String refId;

        /**
         * MISA's internal reference number
         */
        @JsonProperty("ref_no")
        private String refNo;

        /**
         * Voucher type if applicable
         */
        @JsonProperty("voucher_type")
        private Integer voucherType;

        /**
         * Branch ID
         */
        @JsonProperty("branch_id")
        private String branchId;

        /**
         * List of detail items (for vouchers with line items)
         */
        @JsonProperty("detail")
        private List<DetailItem> detail;

        /**
         * Dictionary data (for master data operations)
         */
        @JsonProperty("dictionary")
        private List<DictionaryItem> dictionary;

        /**
         * Additional custom data
         */
        @JsonProperty("custom_data")
        private Map<String, Object> customData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailItem {

        @JsonProperty("org_refid_detail")
        private String orgRefIdDetail;

        @JsonProperty("ref_id_detail")
        private String refIdDetail;

        @JsonProperty("inventory_item_id")
        private String inventoryItemId;

        @JsonProperty("inventory_item_code")
        private String inventoryItemCode;

        @JsonProperty("quantity")
        private Double quantity;

        @JsonProperty("unit_price")
        private Double unitPrice;

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictionaryItem {

        @JsonProperty("dictionary_type")
        private Integer dictionaryType;

        @JsonProperty("org_id")
        private String orgId;

        @JsonProperty("misa_id")
        private String misaId;

        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("success")
        private Boolean success;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_message")
        private String errorMessage;
    }
}
