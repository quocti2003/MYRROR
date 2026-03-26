package com.mirror.product.printing.controller;

import com.mirror.product.printing.dto.InvoiceDataDto;
import com.mirror.product.printing.dto.InvoiceExtractResponse;
import com.mirror.product.printing.service.PdfInvoiceExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/printing")
@RequiredArgsConstructor
public class InvoiceExtractController {

    private final PdfInvoiceExtractorService pdfExtractorService;

    @PostMapping(value = "/extract-invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceExtractResponse> extractInvoice(
            @RequestParam(value = "pdf", required = false) MultipartFile pdfFile) {

        if (pdfFile == null || pdfFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(InvoiceExtractResponse.error("No PDF file uploaded. Use form field name \"pdf\""));
        }

        String filename = pdfFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest()
                    .body(InvoiceExtractResponse.error("File must be a PDF"));
        }

        try {
            InvoiceDataDto invoiceData = pdfExtractorService.extractInvoice(pdfFile);
            return ResponseEntity.ok(InvoiceExtractResponse.success(invoiceData));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid PDF: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(InvoiceExtractResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error extracting invoice", e);
            return ResponseEntity.internalServerError()
                    .body(InvoiceExtractResponse.error("Failed to extract invoice: " + e.getMessage()));
        }
    }

}
