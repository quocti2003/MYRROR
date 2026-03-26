package com.mirror.product.service;

import com.mirror.product.dto.label.LabelPreviewResponse;
import com.mirror.product.entity.LabelTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Service for generating label previews using the Labelary API
 *
 * Labelary is a free ZPL rendering service that converts ZPL to PNG images.
 * API documentation: http://labelary.com/service.html
 */
@Service
@Slf4j
public class LabelaryPreviewService {

    private static final String LABELARY_API_BASE = "http://api.labelary.com/v1/printers";

    private final RestTemplate restTemplate;

    public LabelaryPreviewService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generate a preview image for a ZPL string using Labelary API
     *
     * @param zpl The rendered ZPL string
     * @param dpi Printer DPI (commonly 203, 300, or 600)
     * @param widthInches Label width in inches
     * @param heightInches Label height in inches
     * @return Base64-encoded PNG image
     */
    public String generatePreview(String zpl, int dpi, double widthInches, double heightInches) {
        try {
            // Convert DPI to DPMM (dots per millimeter) - Labelary uses dpmm, not dpi
            // 203 DPI = 8 dpmm, 300 DPI = 12 dpmm, 600 DPI = 24 dpmm
            int dpmm = convertDpiToDpmm(dpi);

            // Labelary API URL format: /v1/printers/{dpmm}dpmm/labels/{width}x{height}/0/
            String url = String.format("%s/%ddpmm/labels/%.1fx%.1f/0/",
                    LABELARY_API_BASE, dpmm, widthInches, heightInches);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "image/png");

            HttpEntity<String> request = new HttpEntity<>(zpl, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Base64.getEncoder().encodeToString(response.getBody());
            } else {
                log.warn("Labelary API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error generating preview from Labelary API: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate a preview response for a template with rendered ZPL
     *
     * @param template The label template
     * @param renderedZpl The ZPL with variables substituted
     * @return Preview response with image and metadata
     */
    public LabelPreviewResponse generatePreviewResponse(LabelTemplate template, String renderedZpl) {
        int dpi = template.getDpi() != null ? template.getDpi() : 203;

        // Convert mm to inches (1 inch = 25.4 mm)
        double widthInches = template.getWidthMm() != null ? template.getWidthMm() / 25.4 : 2.0;
        double heightInches = template.getHeightMm() != null ? template.getHeightMm() / 25.4 : 1.0;

        String previewBase64 = generatePreview(renderedZpl, dpi, widthInches, heightInches);

        return LabelPreviewResponse.builder()
                .renderedZpl(renderedZpl)
                .previewImageBase64(previewBase64)
                .widthMm(template.getWidthMm())
                .heightMm(template.getHeightMm())
                .dpi(dpi)
                .templateId(template.getId())
                .templateName(template.getName())
                .build();
    }

    /**
     * Generate a preview URL (non-cached direct link to Labelary)
     * Note: This URL is for one-time use and may not persist
     *
     * @param dpi Printer DPI
     * @param widthInches Label width in inches
     * @param heightInches Label height in inches
     * @return URL to the Labelary preview endpoint
     */
    public String getPreviewUrl(int dpi, double widthInches, double heightInches) {
        int dpmm = convertDpiToDpmm(dpi);
        return String.format("%s/%ddpmm/labels/%.1fx%.1f/0/",
                LABELARY_API_BASE, dpmm, widthInches, heightInches);
    }

    /**
     * Convert DPI (dots per inch) to DPMM (dots per millimeter)
     * Labelary API uses dpmm values: 6, 8, 12, or 24
     *
     * @param dpi The printer DPI
     * @return The corresponding dpmm value
     */
    private int convertDpiToDpmm(int dpi) {
        // Standard conversions:
        // 152 DPI = 6 dpmm
        // 203 DPI = 8 dpmm
        // 300 DPI = 12 dpmm
        // 600 DPI = 24 dpmm
        if (dpi <= 152) return 6;
        if (dpi <= 203) return 8;
        if (dpi <= 300) return 12;
        return 24;
    }
}
