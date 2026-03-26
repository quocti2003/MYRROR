package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sent back to MISA after processing a callback
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisaCallbackResponse {

    /**
     * Whether the callback was processed successfully
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * Error code if processing failed
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * Human-readable error message
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * Create a successful response
     */
    public static MisaCallbackResponse success() {
        return MisaCallbackResponse.builder()
                .success(true)
                .build();
    }

    /**
     * Create an error response
     */
    public static MisaCallbackResponse error(String errorCode, String errorMessage) {
        return MisaCallbackResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Standard error codes
     */
    public static class ErrorCodes {
        public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
        public static final String INVALID_APP_ID = "INVALID_APP_ID";
        public static final String INVALID_DATA_TYPE = "INVALID_DATA_TYPE";
        public static final String PROCESSING_ERROR = "PROCESSING_ERROR";
        public static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";
    }
}
