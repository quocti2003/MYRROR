package com.mirror.product.controller;

import com.mirror.product.dto.label.*;
import com.mirror.product.entity.LabelTemplate;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Label rendering and printing operations
 */
@RestController
@RequestMapping("/api/v1/labels")
@RequiredArgsConstructor
@Slf4j
public class LabelController {

    private final LabelTemplateService templateService;
    private final ProductService productService;
    private final ZplRenderingService zplRenderingService;
    private final LabelaryPreviewService labelaryPreviewService;

    /**
     * Render a label with variable substitution
     * POST /api/v1/labels/render
     *
     * Returns the rendered ZPL code ready for printing
     */
    @PostMapping("/render")
    public ResponseEntity<?> renderLabel(@Valid @RequestBody LabelRenderRequest request) {
        try {
            LabelTemplate template = templateService.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.getTemplateId()));

            Map<String, String> variables = request.getVariables() != null
                    ? new HashMap<>(request.getVariables())
                    : new HashMap<>();

            // If product ID is provided, extract variables from product
            if (request.getProductId() != null && !request.getProductId().isBlank()) {
                MirrorProduct product = productService.getProductEntityById(request.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

                Map<String, String> productVars = zplRenderingService.extractProductVariables(product);
                // Product vars are the base, request vars override
                productVars.putAll(variables);
                variables = productVars;
            }

            String renderedZpl = zplRenderingService.renderZpl(template, variables);

            // Handle quantity (repeat the ZPL)
            if (request.getQuantity() != null && request.getQuantity() > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < request.getQuantity(); i++) {
                    sb.append(renderedZpl);
                    if (i < request.getQuantity() - 1) {
                        sb.append("\n");
                    }
                }
                renderedZpl = sb.toString();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("renderedZpl", renderedZpl);
            response.put("templateId", template.getId());
            response.put("templateName", template.getName());
            response.put("quantity", request.getQuantity());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error rendering label: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error rendering label", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while rendering label");
        }
    }

    /**
     * Generate a preview for a label
     * POST /api/v1/labels/preview
     *
     * Returns the rendered ZPL and a PNG preview image
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewLabel(@Valid @RequestBody LabelRenderRequest request) {
        try {
            LabelTemplate template = templateService.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.getTemplateId()));

            Map<String, String> variables = request.getVariables() != null
                    ? new HashMap<>(request.getVariables())
                    : new HashMap<>();

            // If product ID is provided, extract variables from product
            if (request.getProductId() != null && !request.getProductId().isBlank()) {
                MirrorProduct product = productService.getProductEntityById(request.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

                Map<String, String> productVars = zplRenderingService.extractProductVariables(product);
                productVars.putAll(variables);
                variables = productVars;
            }

            String renderedZpl = zplRenderingService.renderZpl(template, variables);
            LabelPreviewResponse preview = labelaryPreviewService.generatePreviewResponse(template, renderedZpl);

            return ResponseEntity.ok(preview);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error generating preview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error generating preview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while generating preview");
        }
    }

    /**
     * Batch render labels for multiple products
     * POST /api/v1/labels/render-batch
     *
     * Returns rendered ZPL for all products combined
     */
    @PostMapping("/render-batch")
    public ResponseEntity<?> renderBatchLabels(@Valid @RequestBody LabelPrintRequest request) {
        try {
            LabelTemplate template = templateService.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.getTemplateId()));

            List<String> renderedLabels = new ArrayList<>();
            List<Map<String, Object>> labelDetails = new ArrayList<>();

            for (String productId : request.getProductIds()) {
                MirrorProduct product = productService.getProductEntityById(productId)
                        .orElse(null);

                if (product == null) {
                    log.warn("Product not found: {}, skipping", productId);
                    continue;
                }

                Map<String, String> variables = zplRenderingService.extractProductVariables(product);

                // Apply overrides
                if (request.getVariableOverrides() != null) {
                    variables.putAll(request.getVariableOverrides());
                }

                String renderedZpl = zplRenderingService.renderZpl(template, variables);

                // Handle quantity per product
                int quantity = request.getQuantityPerProduct() != null ? request.getQuantityPerProduct() : 1;
                for (int i = 0; i < quantity; i++) {
                    renderedLabels.add(renderedZpl);
                }

                Map<String, Object> detail = new HashMap<>();
                detail.put("productId", productId);
                detail.put("productName", product.getItemName());
                detail.put("sku", product.getSkuCode());
                detail.put("quantity", quantity);
                labelDetails.add(detail);
            }

            // Combine all ZPL into one string
            String combinedZpl = String.join("\n", renderedLabels);

            Map<String, Object> response = new HashMap<>();
            response.put("renderedZpl", combinedZpl);
            response.put("templateId", template.getId());
            response.put("templateName", template.getName());
            response.put("totalLabels", renderedLabels.size());
            response.put("products", labelDetails);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error rendering batch labels: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error rendering batch labels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while rendering batch labels");
        }
    }

    /**
     * Preview labels for multiple products (returns first product's preview)
     * POST /api/v1/labels/preview-batch
     */
    @PostMapping("/preview-batch")
    public ResponseEntity<?> previewBatchLabels(@Valid @RequestBody LabelPrintRequest request) {
        try {
            LabelTemplate template = templateService.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.getTemplateId()));

            List<LabelPreviewResponse> previews = new ArrayList<>();

            for (String productId : request.getProductIds()) {
                MirrorProduct product = productService.getProductEntityById(productId)
                        .orElse(null);

                if (product == null) {
                    continue;
                }

                Map<String, String> variables = zplRenderingService.extractProductVariables(product);

                if (request.getVariableOverrides() != null) {
                    variables.putAll(request.getVariableOverrides());
                }

                String renderedZpl = zplRenderingService.renderZpl(template, variables);
                LabelPreviewResponse preview = labelaryPreviewService.generatePreviewResponse(template, renderedZpl);

                // Add product info to preview
                previews.add(LabelPreviewResponse.builder()
                        .renderedZpl(preview.getRenderedZpl())
                        .previewImageBase64(preview.getPreviewImageBase64())
                        .previewImageUrl(preview.getPreviewImageUrl())
                        .widthMm(preview.getWidthMm())
                        .heightMm(preview.getHeightMm())
                        .dpi(preview.getDpi())
                        .templateId(template.getId())
                        .templateName(template.getName())
                        .build());

                // Limit preview generation to first 5 products
                if (previews.size() >= 5) {
                    break;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("previews", previews);
            response.put("templateId", template.getId());
            response.put("templateName", template.getName());
            response.put("totalProducts", request.getProductIds().size());
            response.put("previewedProducts", previews.size());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error generating batch previews: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error generating batch previews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while generating batch previews");
        }
    }

    /**
     * Get variables extracted from a product
     * GET /api/v1/labels/product-variables/{productId}
     *
     * Useful for showing what values will be substituted
     */
    @GetMapping("/product-variables/{productId}")
    public ResponseEntity<?> getProductVariables(@PathVariable String productId) {
        try {
            MirrorProduct product = productService.getProductEntityById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

            Map<String, String> variables = zplRenderingService.extractProductVariables(product);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("productName", product.getItemName());
            response.put("sku", product.getSkuCode());
            response.put("variables", variables);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error getting product variables: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting product variables", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while getting product variables");
        }
    }
}
