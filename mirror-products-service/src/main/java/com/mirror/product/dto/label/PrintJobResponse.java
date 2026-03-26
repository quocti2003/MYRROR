package com.mirror.product.dto.label;

import com.mirror.product.entity.label.PrintJob;
import com.mirror.product.entity.label.PrintJobItem;
import com.mirror.product.enums.PrintJobStatus;
import com.mirror.product.enums.PrintMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobResponse {

    private String id;
    private String templateId;
    private String templateName;
    private PrintJobStatus status;
    private Integer totalLabels;
    private Integer printedLabels;
    private Integer failedLabels;
    private String printerName;
    private PrintMethod printMethod;
    private String zplData;
    private String errorMessage;
    private Map<String, Object> options;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private String createdBy;
    private List<PrintJobItemResponse> items;

    public static PrintJobResponse fromEntity(PrintJob entity) {
        return fromEntity(entity, false);
    }

    public static PrintJobResponse fromEntity(PrintJob entity, boolean includeItems) {
        if (entity == null) return null;

        PrintJobResponseBuilder builder = PrintJobResponse.builder()
            .id(entity.getId())
            .templateId(entity.getTemplateId())
            .templateName(entity.getTemplate() != null ? entity.getTemplate().getName() : null)
            .status(entity.getStatus())
            .totalLabels(entity.getTotalLabels())
            .printedLabels(entity.getPrintedLabels())
            .failedLabels(entity.getFailedLabels())
            .printerName(entity.getPrinterName())
            .printMethod(entity.getPrintMethod())
            .zplData(entity.getZplData())
            .errorMessage(entity.getErrorMessage())
            .options(entity.getOptions())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy());

        if (includeItems && entity.getItems() != null) {
            builder.items(entity.getItems().stream()
                .map(PrintJobItemResponse::fromEntity)
                .collect(Collectors.toList()));
        }

        return builder.build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrintJobItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String sku;
        private String epc;
        private String status;
        private String errorMessage;
        private Instant printedAt;

        public static PrintJobItemResponse fromEntity(PrintJobItem entity) {
            if (entity == null) return null;

            return PrintJobItemResponse.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .productName(entity.getProduct() != null ? entity.getProduct().getItemName() : null)
                .sku(entity.getProduct() != null ? entity.getProduct().getSkuCode() : null)
                .epc(entity.getEpc())
                .status(entity.getStatus().name())
                .errorMessage(entity.getErrorMessage())
                .printedAt(entity.getPrintedAt())
                .build();
        }
    }
}
