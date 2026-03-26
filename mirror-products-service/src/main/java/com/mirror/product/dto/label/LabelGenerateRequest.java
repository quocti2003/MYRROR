package com.mirror.product.dto.label;

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
public class LabelGenerateRequest {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    @NotEmpty(message = "At least one product ID is required")
    private List<String> productIds;

    private LabelGenerateOptions options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelGenerateOptions {
        @Builder.Default
        private Boolean includeRFID = true;

        @Builder.Default
        private Integer quantity = 1; // Labels per product

        @Builder.Default
        private Integer darkness = 15; // Print darkness (1-30)

        @Builder.Default
        private Integer speed = 4; // Print speed (1-6 inch/s)

        private Map<String, Object> customData; // Additional data for binding
    }
}
