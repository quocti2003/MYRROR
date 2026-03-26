package com.mirror.product.service;

import com.mirror.product.dto.sourcing.SourcingReportResponse;
import com.mirror.product.model.EmailEnvelope;
import com.mirror.product.service.notification.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service for sending sourcing reports to vendors via email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourcingReportEmailService {

    private final SourcingReportPdfService pdfService;
    private final EmailDeliveryService emailDeliveryService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    /**
     * Send sourcing report to vendor via email with PDF attachment (synchronous)
     */
    public void sendReportToVendor(SourcingReportResponse report) throws IOException {
        if (report.getVendorEmail() == null || report.getVendorEmail().isBlank()) {
            throw new IllegalArgumentException("Vendor email is required to send sourcing report");
        }

        log.info("Sending sourcing report to vendor: {} ({})", report.getVendorName(), report.getVendorEmail());

        EmailEnvelope envelope = buildEmailEnvelope(report);
        emailDeliveryService.send(envelope);
        log.info("Sourcing report sent successfully to: {}", report.getVendorEmail());
    }

    /**
     * Send sourcing report to vendor asynchronously (non-blocking)
     * Returns a CompletableFuture that completes when the email is sent
     */
    @Async
    public CompletableFuture<Boolean> sendReportToVendorAsync(SourcingReportResponse report) {
        if (report.getVendorEmail() == null || report.getVendorEmail().isBlank()) {
            log.warn("Cannot send report async - vendor email is missing for vendor: {}", report.getVendorId());
            return CompletableFuture.completedFuture(false);
        }

        log.info("Sending sourcing report asynchronously to vendor: {} ({})",
                report.getVendorName(), report.getVendorEmail());

        try {
            EmailEnvelope envelope = buildEmailEnvelope(report);
            emailDeliveryService.send(envelope);
            log.info("Sourcing report sent successfully (async) to: {}", report.getVendorEmail());
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send sourcing report (async) to {}: {}",
                    report.getVendorEmail(), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Build email envelope with PDF attachment
     */
    private EmailEnvelope buildEmailEnvelope(SourcingReportResponse report) throws IOException {
        // Generate PDF
        byte[] pdfBytes = pdfService.generatePdf(report);
        String pdfFilename = String.format("sourcing-report-%s-%s.pdf",
                report.getVendorCode(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")));

        // Build email
        String subject = String.format("MIRROR Sourcing Request - %s", report.getProductionPlanName());
        String htmlBody = buildHtmlEmailBody(report);
        String textBody = buildTextEmailBody(report);

        Map<String, byte[]> attachments = new HashMap<>();
        attachments.put(pdfFilename, pdfBytes);

        return EmailEnvelope.builder()
                .recipients(Set.of(report.getVendorEmail()))
                .subject(subject)
                .htmlBody(htmlBody)
                .textBody(textBody)
                .attachments(attachments)
                .build();
    }

    private String buildHtmlEmailBody(SourcingReportResponse report) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".header { background: #1a1a2e; color: white; padding: 20px; text-align: center; }");
        html.append(".content { padding: 20px; }");
        html.append(".section { margin-bottom: 20px; }");
        html.append(".section h3 { color: #1a1a2e; border-bottom: 2px solid #bc224c; padding-bottom: 5px; }");
        html.append(".info-row { margin: 8px 0; }");
        html.append(".label { font-weight: bold; color: #666; }");
        html.append(".value { color: #333; }");
        html.append(".summary-box { background: #f5f7fa; padding: 15px; border-radius: 8px; }");
        html.append(".footer { background: #f0f0f0; padding: 15px; text-align: center; font-size: 12px; color: #666; }");
        html.append("</style></head><body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>MIRROR Diamond</h1>");
        html.append("<h2>Sourcing Request</h2>");
        html.append("</div>");

        // Content
        html.append("<div class='content'>");

        // Greeting
        html.append("<p>Dear ").append(report.getVendorName()).append(",</p>");
        html.append("<p>Please find attached the sourcing report for the production plan: <strong>")
            .append(report.getProductionPlanName()).append("</strong>.</p>");

        // Plan summary section
        html.append("<div class='section'>");
        html.append("<h3>Production Plan Details</h3>");
        html.append("<div class='info-row'><span class='label'>Plan Name:</span> <span class='value'>")
            .append(report.getProductionPlanName()).append("</span></div>");
        if (report.getTargetStartDate() != null) {
            html.append("<div class='info-row'><span class='label'>Target Start:</span> <span class='value'>")
                .append(report.getTargetStartDate().format(DATE_FORMAT)).append("</span></div>");
        }
        if (report.getTargetEndDate() != null) {
            html.append("<div class='info-row'><span class='label'>Target End:</span> <span class='value'>")
                .append(report.getTargetEndDate().format(DATE_FORMAT)).append("</span></div>");
        }
        html.append("</div>");

        // Summary box
        html.append("<div class='section'>");
        html.append("<h3>Order Summary</h3>");
        html.append("<div class='summary-box'>");
        html.append("<div class='info-row'><span class='label'>Total Orders:</span> <span class='value'>")
            .append(report.getTotalOrders()).append("</span></div>");
        html.append("<div class='info-row'><span class='label'>Total Quantity:</span> <span class='value'>")
            .append(report.getTotalQuantity()).append(" pieces</span></div>");
        if (report.getEstimatedTotalCost() != null) {
            html.append("<div class='info-row'><span class='label'>Estimated Cost:</span> <span class='value'>")
                .append(String.format("%,.0f VND", report.getEstimatedTotalCost())).append("</span></div>");
        }
        html.append("</div>");
        html.append("</div>");

        // Instructions
        html.append("<div class='section'>");
        html.append("<h3>Next Steps</h3>");
        html.append("<ol>");
        html.append("<li>Review the attached PDF for detailed specifications</li>");
        html.append("<li>Prepare the required materials as listed in the report</li>");
        html.append("<li>Confirm your availability by responding to this email</li>");
        html.append("<li>Contact us if you have any questions about the specifications</li>");
        html.append("</ol>");
        html.append("</div>");

        html.append("<p>Please confirm receipt of this sourcing request and your estimated timeline.</p>");
        html.append("<p>Best regards,<br/>MIRROR Diamond Production Team</p>");

        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>This is an automated message from MIRROR Diamond Production System.</p>");
        html.append("<p>Report generated on: ").append(report.getGeneratedAt().format(DATETIME_FORMAT)).append("</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private String buildTextEmailBody(SourcingReportResponse report) {
        StringBuilder text = new StringBuilder();
        text.append("MIRROR Diamond - Sourcing Request\n");
        text.append("================================\n\n");

        text.append("Dear ").append(report.getVendorName()).append(",\n\n");
        text.append("Please find attached the sourcing report for the production plan: ")
            .append(report.getProductionPlanName()).append(".\n\n");

        text.append("PRODUCTION PLAN DETAILS\n");
        text.append("-----------------------\n");
        text.append("Plan Name: ").append(report.getProductionPlanName()).append("\n");
        if (report.getTargetStartDate() != null) {
            text.append("Target Start: ").append(report.getTargetStartDate().format(DATE_FORMAT)).append("\n");
        }
        if (report.getTargetEndDate() != null) {
            text.append("Target End: ").append(report.getTargetEndDate().format(DATE_FORMAT)).append("\n");
        }
        text.append("\n");

        text.append("ORDER SUMMARY\n");
        text.append("-------------\n");
        text.append("Total Orders: ").append(report.getTotalOrders()).append("\n");
        text.append("Total Quantity: ").append(report.getTotalQuantity()).append(" pieces\n");
        if (report.getEstimatedTotalCost() != null) {
            text.append("Estimated Cost: ").append(String.format("%,.0f VND", report.getEstimatedTotalCost())).append("\n");
        }
        text.append("\n");

        text.append("NEXT STEPS\n");
        text.append("----------\n");
        text.append("1. Review the attached PDF for detailed specifications\n");
        text.append("2. Prepare the required materials as listed in the report\n");
        text.append("3. Confirm your availability by responding to this email\n");
        text.append("4. Contact us if you have any questions about the specifications\n\n");

        text.append("Please confirm receipt of this sourcing request and your estimated timeline.\n\n");
        text.append("Best regards,\n");
        text.append("MIRROR Diamond Production Team\n\n");

        text.append("---\n");
        text.append("This is an automated message from MIRROR Diamond Production System.\n");
        text.append("Report generated on: ").append(report.getGeneratedAt().format(DATETIME_FORMAT)).append("\n");

        return text.toString();
    }
}
