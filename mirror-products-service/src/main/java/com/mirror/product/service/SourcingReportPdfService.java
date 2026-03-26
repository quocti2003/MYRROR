package com.mirror.product.service;

import com.mirror.product.dto.sourcing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating PDF exports of sourcing reports
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourcingReportPdfService {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 14;
    private static final float SECTION_SPACING = 20;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    /**
     * Generate PDF bytes for a sourcing report
     */
    public byte[] generatePdf(SourcingReportResponse report) throws IOException {
        // Validate required fields
        if (report == null) {
            throw new IllegalArgumentException("Sourcing report cannot be null");
        }
        if (report.getReportId() == null || report.getReportId().isBlank()) {
            throw new IllegalArgumentException("Report ID is required");
        }

        log.info("Generating PDF for sourcing report: {} - vendor: {}",
                report.getReportId(),
                nullSafe(report.getVendorName(), "Unknown Vendor"));

        try (PDDocument document = new PDDocument()) {
            float yPosition = addReportContent(document, report);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Error generating PDF for report {}: {}", report.getReportId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Null-safe string accessor with default value
     */
    private String nullSafe(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /**
     * Null-safe string accessor, returns "N/A" if null
     */
    private String nullSafe(String value) {
        return nullSafe(value, "N/A");
    }

    private float addReportContent(PDDocument document, SourcingReportResponse report) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float contentWidth = pageWidth - 2 * MARGIN;
        float yPosition = pageHeight - MARGIN;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Header
            yPosition = addHeader(contentStream, report, fontBold, fontRegular, yPosition, contentWidth);

            // Partner Info
            yPosition = addPartnerInfo(contentStream, report, fontBold, fontRegular, yPosition);

            // Plan Info
            yPosition = addPlanInfo(contentStream, report, fontBold, fontRegular, yPosition);

            // Summary
            yPosition = addSummary(contentStream, report, fontBold, fontRegular, yPosition);
        }

        // Add order items on subsequent pages if needed
        yPosition = addOrderItems(document, report, fontBold, fontRegular);

        // Add material summary
        addMaterialSummary(document, report, fontBold, fontRegular);

        return yPosition;
    }

    private float addHeader(PDPageContentStream cs, SourcingReportResponse report,
                           PDType1Font fontBold, PDType1Font fontRegular,
                           float yPosition, float contentWidth) throws IOException {
        // Title
        cs.beginText();
        cs.setFont(fontBold, 20);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("SOURCING REPORT");
        cs.endText();
        yPosition -= LINE_HEIGHT * 1.5f;

        // Report ID and Date
        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Report ID: " + report.getReportId());
        cs.endText();
        yPosition -= LINE_HEIGHT;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Generated: " + (report.getGeneratedAt() != null ? report.getGeneratedAt().format(DATETIME_FORMAT) : "N/A"));
        cs.endText();
        yPosition -= LINE_HEIGHT;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Generated By: " + (report.getGeneratedBy() != null ? report.getGeneratedBy() : "System"));
        cs.endText();
        yPosition -= SECTION_SPACING;

        // Divider line
        cs.moveTo(MARGIN, yPosition);
        cs.lineTo(MARGIN + contentWidth, yPosition);
        cs.stroke();
        yPosition -= SECTION_SPACING;

        return yPosition;
    }

    private float addPartnerInfo(PDPageContentStream cs, SourcingReportResponse report,
                                PDType1Font fontBold, PDType1Font fontRegular,
                                float yPosition) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Partner Information");
        cs.endText();
        yPosition -= LINE_HEIGHT * 1.5f;

        String[][] partnerFields = {
            {"Name:", nullSafe(report.getVendorName(), "Unknown Vendor")},
            {"Code:", nullSafe(report.getVendorCode(), "N/A")},
            {"Email:", nullSafe(report.getVendorEmail())},
            {"Phone:", nullSafe(report.getVendorPhone())},
            {"Country:", nullSafe(report.getVendorCountry())}
        };

        for (String[] field : partnerFields) {
            cs.beginText();
            cs.setFont(fontBold, 10);
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText(field[0]);
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(80, 0);
            cs.showText(field[1] != null ? field[1] : "N/A");
            cs.endText();
            yPosition -= LINE_HEIGHT;
        }

        yPosition -= SECTION_SPACING / 2;
        return yPosition;
    }

    private float addPlanInfo(PDPageContentStream cs, SourcingReportResponse report,
                             PDType1Font fontBold, PDType1Font fontRegular,
                             float yPosition) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Production Plan");
        cs.endText();
        yPosition -= LINE_HEIGHT * 1.5f;

        String[][] planFields = {
            {"Plan Name:", nullSafe(report.getProductionPlanName(), "Unnamed Plan")},
            {"Plan ID:", nullSafe(report.getProductionPlanId())},
            {"Target Start:", report.getTargetStartDate() != null ? report.getTargetStartDate().format(DATE_FORMAT) : "TBD"},
            {"Target End:", report.getTargetEndDate() != null ? report.getTargetEndDate().format(DATE_FORMAT) : "TBD"}
        };

        for (String[] field : planFields) {
            cs.beginText();
            cs.setFont(fontBold, 10);
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText(field[0]);
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(100, 0);
            cs.showText(field[1] != null ? field[1] : "N/A");
            cs.endText();
            yPosition -= LINE_HEIGHT;
        }

        yPosition -= SECTION_SPACING / 2;
        return yPosition;
    }

    private float addSummary(PDPageContentStream cs, SourcingReportResponse report,
                            PDType1Font fontBold, PDType1Font fontRegular,
                            float yPosition) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Summary");
        cs.endText();
        yPosition -= LINE_HEIGHT * 1.5f;

        String[][] summaryFields = {
            {"Total Orders:", String.valueOf(report.getTotalOrders())},
            {"Total Quantity:", String.valueOf(report.getTotalQuantity())},
            {"Estimated Cost:", formatCurrency(report.getEstimatedTotalCost())}
        };

        for (String[] field : summaryFields) {
            cs.beginText();
            cs.setFont(fontBold, 10);
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText(field[0]);
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(120, 0);
            cs.showText(field[1]);
            cs.endText();
            yPosition -= LINE_HEIGHT;
        }

        yPosition -= SECTION_SPACING;
        return yPosition;
    }

    private float addOrderItems(PDDocument document, SourcingReportResponse report,
                               PDType1Font fontBold, PDType1Font fontRegular) throws IOException {
        if (report.getOrders() == null || report.getOrders().isEmpty()) {
            return 0;
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();
        float contentWidth = pageWidth - 2 * MARGIN;
        float yPosition = pageHeight - MARGIN;

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            cs.beginText();
            cs.setFont(fontBold, 16);
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText("Order Details");
            cs.endText();
            yPosition -= SECTION_SPACING;

            for (SourcingOrderItem order : report.getOrders()) {
                // Check if we need a new page
                if (yPosition < MARGIN + 150) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    yPosition = pageHeight - MARGIN;
                    // Create new content stream for new page
                    PDPageContentStream newCs = new PDPageContentStream(document, page);
                    yPosition = addOrderItemContent(newCs, order, fontBold, fontRegular, yPosition, contentWidth);
                    newCs.close();
                } else {
                    yPosition = addOrderItemContent(cs, order, fontBold, fontRegular, yPosition, contentWidth);
                }
            }
        }

        return yPosition;
    }

    private float addOrderItemContent(PDPageContentStream cs, SourcingOrderItem order,
                                     PDType1Font fontBold, PDType1Font fontRegular,
                                     float yPosition, float contentWidth) throws IOException {
        if (order == null) {
            return yPosition;
        }

        // Order header
        cs.beginText();
        cs.setFont(fontBold, 12);
        cs.newLineAtOffset(MARGIN, yPosition);
        cs.showText("Order: " + nullSafe(order.getOrderNumber(), "Unknown"));
        cs.endText();
        yPosition -= LINE_HEIGHT;

        // Order details
        String stageName = nullSafe(order.getStageName(), "Unknown Stage");
        Integer stageOrder = order.getStageOrder() != null ? order.getStageOrder() : 0;
        String[][] orderFields = {
            {"Quantity:", String.valueOf(order.getQuantity())},
            {"Stage:", stageName + " (#" + stageOrder + ")"},
            {"JTRC:", nullSafe(order.getReportNumber())},
            {"Category:", nullSafe(order.getCategory())},
            {"Collection:", nullSafe(order.getCollection())}
        };

        for (String[] field : orderFields) {
            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(MARGIN + 10, yPosition);
            cs.showText(field[0]);
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(80, 0);
            cs.showText(field[1]);
            cs.endText();
            yPosition -= LINE_HEIGHT;
        }

        // Metal spec
        if (order.getMetalSpec() != null) {
            SourcingMetalSpec metal = order.getMetalSpec();
            String metalType = nullSafe(metal.getMetalType(), "Unknown");
            String purity = nullSafe(metal.getPurity(), "N/A");
            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(MARGIN + 10, yPosition);
            cs.showText("Metal: " + metalType + " " + purity +
                       " - " + formatWeight(metal.getTotalWeight()) + "g");
            cs.endText();
            yPosition -= LINE_HEIGHT;
        }

        // Stone specs
        if (order.getStoneSpecs() != null && !order.getStoneSpecs().isEmpty()) {
            for (SourcingStoneSpec stone : order.getStoneSpecs()) {
                if (stone == null) continue;
                String stoneType = nullSafe(stone.getStoneType(), "Unknown");
                String shape = nullSafe(stone.getShape(), "N/A");
                int quantity = stone.getQuantity() != null ? stone.getQuantity() : 0;
                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(MARGIN + 10, yPosition);
                cs.showText("Stone: " + stoneType + " " + shape +
                           " - " + quantity + " pcs, " + formatCarat(stone.getTotalCaratWeight()) + " ct");
                cs.endText();
                yPosition -= LINE_HEIGHT;
            }
        }

        yPosition -= LINE_HEIGHT;

        // Divider
        cs.moveTo(MARGIN, yPosition);
        cs.lineTo(MARGIN + contentWidth, yPosition);
        cs.setLineWidth(0.5f);
        cs.stroke();
        yPosition -= LINE_HEIGHT;

        return yPosition;
    }

    private void addMaterialSummary(PDDocument document, SourcingReportResponse report,
                                   PDType1Font fontBold, PDType1Font fontRegular) throws IOException {
        if (report.getMaterialSummary() == null) {
            return;
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();
        float contentWidth = pageWidth - 2 * MARGIN;
        float yPosition = pageHeight - MARGIN;

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            cs.beginText();
            cs.setFont(fontBold, 16);
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText("Material Summary");
            cs.endText();
            yPosition -= SECTION_SPACING;

            MaterialAggregationSummary summary = report.getMaterialSummary();

            // Metals
            if (summary.getMetals() != null && !summary.getMetals().isEmpty()) {
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(MARGIN, yPosition);
                cs.showText("Metal Requirements");
                cs.endText();
                yPosition -= LINE_HEIGHT;

                for (MaterialAggregationSummary.MetalAggregation metal : summary.getMetals()) {
                    if (metal == null) continue;
                    String metalType = nullSafe(metal.getMetalType(), "Unknown");
                    String purity = nullSafe(metal.getPurity(), "N/A");
                    int orderCount = metal.getOrderCount() != null ? metal.getOrderCount() : 0;
                    cs.beginText();
                    cs.setFont(fontRegular, 10);
                    cs.newLineAtOffset(MARGIN + 10, yPosition);
                    cs.showText(metalType + " " + purity + ": " +
                               formatWeight(metal.getTotalWeight()) + "g (" + orderCount + " orders)");
                    cs.endText();
                    yPosition -= LINE_HEIGHT;
                }
                yPosition -= LINE_HEIGHT;
            }

            // Stones
            if (summary.getStones() != null && !summary.getStones().isEmpty()) {
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(MARGIN, yPosition);
                cs.showText("Stone Requirements");
                cs.endText();
                yPosition -= LINE_HEIGHT;

                for (MaterialAggregationSummary.StoneAggregation stone : summary.getStones()) {
                    if (stone == null) continue;
                    String stoneType = nullSafe(stone.getStoneType(), "Unknown");
                    String shape = nullSafe(stone.getShape(), "N/A");
                    int totalQty = stone.getTotalQuantity() != null ? stone.getTotalQuantity() : 0;
                    cs.beginText();
                    cs.setFont(fontRegular, 10);
                    cs.newLineAtOffset(MARGIN + 10, yPosition);
                    cs.showText(stoneType + " " + shape + ": " +
                               totalQty + " pcs, " + formatCarat(stone.getTotalCaratWeight()) + " ct");
                    cs.endText();
                    yPosition -= LINE_HEIGHT;
                }
                yPosition -= LINE_HEIGHT;
            }

            // Cost totals
            cs.beginText();
            cs.setFont(fontBold, 12);
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText("Cost Summary");
            cs.endText();
            yPosition -= LINE_HEIGHT;

            String[][] costFields = {
                {"Metal Cost:", formatCurrency(summary.getTotalMetalCost())},
                {"Stone Cost:", formatCurrency(summary.getTotalStoneCost())},
                {"Labor Cost:", formatCurrency(summary.getTotalLaborCost())},
                {"Grand Total:", formatCurrency(summary.getGrandTotal())}
            };

            for (String[] field : costFields) {
                cs.beginText();
                cs.setFont(fontBold, 10);
                cs.newLineAtOffset(MARGIN + 10, yPosition);
                cs.showText(field[0]);
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(100, 0);
                cs.showText(field[1]);
                cs.endText();
                yPosition -= LINE_HEIGHT;
            }
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "N/A";
        return String.format("%,.0f VND", amount);
    }

    private String formatWeight(BigDecimal weight) {
        if (weight == null) return "0.00";
        return String.format("%.2f", weight);
    }

    private String formatCarat(BigDecimal carat) {
        if (carat == null) return "0.00";
        return String.format("%.2f", carat);
    }
}
