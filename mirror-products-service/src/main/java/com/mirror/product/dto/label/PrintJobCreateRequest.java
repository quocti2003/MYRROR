package com.mirror.product.dto.label;

import com.mirror.product.enums.PrintMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobCreateRequest {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    @NotEmpty(message = "At least one product ID is required")
    private List<String> productIds;

    private String printerName;

    @Builder.Default
    private PrintMethod printMethod = PrintMethod.NETWORK;

    private PrintJobOptions options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrintJobOptions {
        @Builder.Default
        private Boolean includeRFID = true;

        @Builder.Default
        private Integer copies = 1;

        @Builder.Default
        private Integer darkness = 15;

        @Builder.Default
        private Integer speed = 4;

        private Map<String, Object> additionalOptions;
    }
}
