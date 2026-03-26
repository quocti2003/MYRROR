package com.mirror.product.controller.label;

import com.mirror.product.dto.label.PrintRequest;
import com.mirror.product.service.label.PrinterService;
import com.mirror.product.service.label.PrintJobService;
import com.mirror.product.service.label.ZPLGeneratorService;
import com.mirror.product.dto.label.PrintJobResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for printer operations
 * - Send ZPL to printer via Network (TCP/IP) or USB (Windows Spooler)
 */
@RestController
@RequestMapping("/api/v1/admin/printer")
@RequiredArgsConstructor
@Slf4j
public class PrinterController {

    private final PrinterService printerService;
    private final PrintJobService printJobService;
    private final ZPLGeneratorService zplGeneratorService;

    /**
     * Get list of available printers (USB/Spooler)
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> getAvailablePrinters() {
        String[] printers = printerService.getAvailablePrintersArray();
        Map<String, Object> response = new HashMap<>();
        response.put("printers", printers);
        response.put("count", printers.length);
        return ResponseEntity.ok(response);
    }

    /**
     * Test printer connection
     */
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> testConnection(@Valid @RequestBody PrintRequest request) {
        PrinterService.PrintResult result = printerService.testConnection(
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("printMethod", request.getPrintMethod());
        response.put("printerAddress", request.getPrinterAddress());

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Send print job to printer
     */
    @PostMapping("/jobs/{jobId}/print")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> printJob(
            @PathVariable String jobId,
            @Valid @RequestBody PrintRequest request) {

        log.info("Printing job {} to {} via {}", jobId, request.getPrinterAddress(), request.getPrintMethod());

        // Get print job with ZPL data
        PrintJobResponse job = printJobService.getById(jobId, false);
        if (job.getZplData() == null || job.getZplData().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Print job has no ZPL data");
            return ResponseEntity.badRequest().body(error);
        }

        // Send to printer
        PrinterService.PrintResult result = printerService.sendToPrinter(
            job.getZplData(),
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("jobId", jobId);
        response.put("totalLabels", job.getTotalLabels());
        response.put("printMethod", request.getPrintMethod());
        response.put("printerAddress", request.getPrinterAddress());

        if (result.success()) {
            // Mark job as completed (ZPL was sent to printer successfully)
            try {
                printJobService.forceComplete(jobId, "printer-service");
                response.put("status", "COMPLETED");
            } catch (Exception e) {
                log.warn("Could not update job status: {}", e.getMessage());
                response.put("status", "SENT_TO_PRINTER");
            }
        }

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Send ZPL directly to printer (without creating a job)
     */
    @PostMapping("/send-zpl")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> sendZplDirect(
            @RequestParam String zplData,
            @Valid @RequestBody PrintRequest request) {

        log.info("Sending ZPL directly to {} via {}", request.getPrinterAddress(), request.getPrintMethod());

        PrinterService.PrintResult result = printerService.sendToPrinter(
            zplData,
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("zplLength", zplData.length());

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Send ZPL directly via request body (for long ZPL from frontend editor)
     */
    @PostMapping("/send-zpl-body")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> sendZplDirectBody(@RequestBody Map<String, Object> request) {
        String zplData = (String) request.get("zplData");
        String printMethodStr = (String) request.get("printMethod");
        String printerAddress = (String) request.get("printerAddress");

        if (zplData == null || zplData.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "zplData is required");
            return ResponseEntity.badRequest().body(error);
        }

        com.mirror.product.enums.PrintMethod printMethod;
        try {
            printMethod = com.mirror.product.enums.PrintMethod.valueOf(printMethodStr);
        } catch (Exception e) {
            printMethod = com.mirror.product.enums.PrintMethod.SPOOLER;
        }

        log.info("Sending ZPL (body) to {} via {}, length={}", printerAddress, printMethod, zplData.length());

        PrinterService.PrintResult result = printerService.sendToPrinter(zplData, printMethod, printerAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("zplLength", zplData.length());

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Print test label
     */
    @PostMapping("/test-print")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> printTestLabel(@Valid @RequestBody PrintRequest request) {
        // Simple test label ZPL
        String testZpl = """
            ^XA
            ^FO50,50^A0N,40,40^FDMirror Diamond^FS
            ^FO50,100^A0N,30,30^FDTest Label^FS
            ^FO50,150^A0N,20,20^FDPrinter OK!^FS
            ^FO50,200^GB400,3,3^FS
            ^FO50,220^A0N,20,20^FDMethod: %s^FS
            ^FO50,250^A0N,20,20^FDAddress: %s^FS
            ^PQ1
            ^XZ
            """.formatted(request.getPrintMethod(), request.getPrinterAddress());

        log.info("Printing test label to {} via {}", request.getPrinterAddress(), request.getPrintMethod());

        PrinterService.PrintResult result = printerService.sendToPrinter(
            testZpl,
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("testZpl", testZpl);

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Print barcode-only test (no RFID)
     */
    @PostMapping("/test-barcode")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> printBarcodeTest(
            @RequestParam(defaultValue = "1234567890128") String barcode,
            @Valid @RequestBody PrintRequest request) {

        // Simple barcode label - EAN-13 format
        String barcodeZpl = """
            ^XA
            ^PW560
            ^LL320
            ^LH0,0
            ^FO80,80
            ^BEN,100,Y,N
            ^FD%s^FS
            ^PQ1
            ^XZ
            """.formatted(barcode);

        log.info("Printing barcode test {} to {} via {}", barcode, request.getPrinterAddress(), request.getPrintMethod());

        PrinterService.PrintResult result = printerService.sendToPrinter(
            barcodeZpl,
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("barcode", barcode);
        response.put("zpl", barcodeZpl);

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Print using TSPL (for UROVO and TSC printers)
     */
    @PostMapping("/test-tspl")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> printTsplTest(
            @RequestParam(defaultValue = "1234567890128") String barcode,
            @Valid @RequestBody PrintRequest request) {

        // TSPL format for UROVO/TSC printers
        String tspl = """
            SIZE 70 mm, 40 mm
            GAP 3 mm, 0 mm
            DIRECTION 1
            CLS
            BARCODE 80,80,"EAN13",100,1,0,2,2,"%s"
            PRINT 1
            """.formatted(barcode);

        log.info("Printing TSPL barcode {} to {} via {}", barcode, request.getPrinterAddress(), request.getPrintMethod());

        PrinterService.PrintResult result = printerService.sendToPrinter(
            tspl,
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("barcode", barcode);
        response.put("tspl", tspl);
        response.put("note", "TSPL format for UROVO/TSC printers");

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Word wrap text by words, using \& as ZPL line break
     * @param text input text
     * @param maxCharsPerLine max characters per line
     * @return wrapped text with \& line breaks
     */
    private String wrapTextByWord(String text, int maxCharsPerLine) {
        if (text == null || text.length() <= maxCharsPerLine) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxCharsPerLine) {
                currentLine.append(" ").append(word);
            } else {
                // Line is full, add line break and start new line
                result.append(currentLine).append("\\&");
                currentLine = new StringBuilder(word);
            }
        }
        // Add last line
        result.append(currentLine);

        return result.toString();
    }

    /**
     * Print jewelry label (95x50mm) - with optional RFID encoding
     * Label: 95mm width x 50mm height (landscape/horizontal)
     * At 203 DPI (8 dots/mm): 760 x 400 dots
     * All positions are FIXED. Product name auto-wraps using ^FB (Field Block)
     */
    @PostMapping("/print-jewelry-label")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> printJewelryLabel(
            @RequestParam(defaultValue = "10.000.000") String price,
            @RequestParam(defaultValue = "") String productName,
            @RequestParam(defaultValue = "123456789012") String barcode,
            @RequestParam(defaultValue = "false") boolean includeRFID,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String skuCode,
            @Valid @RequestBody PrintRequest request) {

        // Word wrap product name (max ~18 chars per line for font 24 and width 250)
        String wrappedProductName = wrapTextByWord(productName, 18);

        // Generate EPC if RFID is enabled
        String epc = null;
        String rfidCommands = "";
        if (includeRFID) {
            // Use barcode as fallback for productId/skuCode if not provided
            String effectiveProductId = (productId != null && !productId.isEmpty()) ? productId : barcode;
            String effectiveSkuCode = (skuCode != null && !skuCode.isEmpty()) ? skuCode : barcode;

            epc = zplGeneratorService.generateEPC(effectiveProductId, effectiveSkuCode);
            log.info("Generated EPC for jewelry label: {}", epc);

            // RFID encoding commands - QUAN TRỌNG:
            // ^RFW,H^FD<epc>^FS ghi vào chip RFID, KHÔNG in lên label
            // Chỉ in lên label khi có ^FO (Field Origin) trước ^FD
            rfidCommands = "^RS8\n^RFW,H^FD" + epc + "^FS\n";
        }

        // Jewelry label ZPL for 95x50mm (landscape)
        // Width: 95mm = 760 dots, Height: 50mm = 400 dots
        // Layout: All positions FIXED - Product Name (y=70) -> Price (y=170) -> Barcode (y=220)
        String zpl = """
            ^XA
            ^PW760
            ^LL400
            ^MD15
            ^PR4
            ^LH0,0
            %s
            ^FO480,70
            ^A0N,24,24
            ^FB250,2,0,L,0
            ^FD%s^FS

            ^FO480,170
            ^A0N,28,28
            ^FD%s VND^FS

            ^FO470,220
            ^BY3
            ^BEN,62,Y,N
            ^FD%s^FS

            ^PQ1
            ^XZ
            """.formatted(rfidCommands, wrappedProductName, price, barcode);

        log.info("Printing jewelry label: price={}, productName={}, barcode={}, includeRFID={}, epc={} to {} via {}",
                price, productName, barcode, includeRFID, epc, request.getPrinterAddress(), request.getPrintMethod());
        log.debug("Generated ZPL:\n{}", zpl);

        PrinterService.PrintResult result = printerService.sendToPrinter(
            zpl,
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("price", price);
        response.put("productName", productName);
        response.put("barcode", barcode);
        response.put("includeRFID", includeRFID);
        response.put("epc", epc);
        response.put("zpl", zpl);

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Preview jewelry label ZPL (for debugging - no print)
     */
    @GetMapping("/preview-jewelry-label")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> previewJewelryLabel(
            @RequestParam(defaultValue = "10.000.000") String price,
            @RequestParam(defaultValue = "Test Product Name") String productName,
            @RequestParam(defaultValue = "123456789012") String barcode,
            @RequestParam(defaultValue = "false") boolean includeRFID,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String skuCode) {

        String wrappedProductName = wrapTextByWord(productName, 18);

        String epc = null;
        String rfidCommands = "";
        if (includeRFID) {
            String effectiveProductId = (productId != null && !productId.isEmpty()) ? productId : barcode;
            String effectiveSkuCode = (skuCode != null && !skuCode.isEmpty()) ? skuCode : barcode;
            epc = zplGeneratorService.generateEPC(effectiveProductId, effectiveSkuCode);
            rfidCommands = "^RS8\n^RFW,H^FD" + epc + "^FS\n";
        }

        String zpl = """
            ^XA
            ^PW760
            ^LL400
            ^MD15
            ^PR4
            ^LH0,0
            %s
            ^FO480,70
            ^A0N,24,24
            ^FB250,2,0,L,0
            ^FD%s^FS

            ^FO480,170
            ^A0N,28,28
            ^FD%s VND^FS

            ^FO470,220
            ^BY3
            ^BEN,62,Y,N
            ^FD%s^FS

            ^PQ1
            ^XZ
            """.formatted(rfidCommands, wrappedProductName, price, barcode);

        Map<String, Object> response = new HashMap<>();
        response.put("price", price);
        response.put("productName", productName);
        response.put("wrappedProductName", wrappedProductName);
        response.put("barcode", barcode);
        response.put("includeRFID", includeRFID);
        response.put("epc", epc);
        response.put("rfidCommands", rfidCommands);
        response.put("zpl", zpl);
        response.put("note", "This is a PREVIEW only - no print sent. Check 'zpl' field for the generated ZPL code.");

        return ResponseEntity.ok(response);
    }

    /**
     * Test RFID write with different formats
     */
    @PostMapping("/test-rfid")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
    public ResponseEntity<Map<String, Object>> testRfidWrite(
            @RequestParam(defaultValue = "1") int format,
            @RequestParam(defaultValue = "300833B2DDD9014000000001") String epc,
            @Valid @RequestBody PrintRequest request) {

        String zpl;
        String formatDesc;

        switch (format) {
            case 1:
                // Standard ZPL RFID write
                formatDesc = "Standard ZPL ^RFW,H";
                zpl = """
                    ^XA
                    ^RS8
                    ^RFW,H^FD%s^FS
                    ^FO50,50^A0N,30,30^FDRFID Test^FS
                    ^FO50,100^A0N,20,20^FDEPC: %s^FS
                    ^PQ1
                    ^XZ
                    """.formatted(epc, epc);
                break;
            case 2:
                // EPC memory bank write
                formatDesc = "EPC Bank ^RFW,H,1,12";
                zpl = """
                    ^XA
                    ^RS8
                    ^RFW,H,1,12^FD%s^FS
                    ^FO50,50^A0N,30,30^FDRFID Test^FS
                    ^FO50,100^A0N,20,20^FDEPC: %s^FS
                    ^PQ1
                    ^XZ
                    """.formatted(epc, epc);
                break;
            case 3:
                // EPC format
                formatDesc = "EPC format ^RFW,E";
                zpl = """
                    ^XA
                    ^RS8
                    ^RFW,E^FD%s^FS
                    ^FO50,50^A0N,30,30^FDRFID Test^FS
                    ^FO50,100^A0N,20,20^FDEPC: %s^FS
                    ^PQ1
                    ^XZ
                    """.formatted(epc, epc);
                break;
            case 4:
                // With RFID setup commands
                formatDesc = "With ^RN,^RF setup";
                zpl = """
                    ^XA
                    ^RN0
                    ^RS8
                    ^RR3
                    ^RFW,H,2,6^FD%s^FS
                    ^FO50,50^A0N,30,30^FDRFID Test^FS
                    ^FO50,100^A0N,20,20^FDEPC: %s^FS
                    ^PQ1
                    ^XZ
                    """.formatted(epc, epc);
                break;
            case 5:
                // No RFID - just barcode (to verify printer works)
                formatDesc = "No RFID - Barcode only";
                zpl = """
                    ^XA
                    ^FO50,50^A0N,30,30^FDBarcode Test^FS
                    ^FO50,100^BCN,80,Y,N,N^FD1234567890^FS
                    ^PQ1
                    ^XZ
                    """;
                break;
            default:
                formatDesc = "Default";
                zpl = """
                    ^XA
                    ^RS8
                    ^RFW,H^FD%s^FS
                    ^FO50,50^A0N,30,30^FDTest^FS
                    ^PQ1
                    ^XZ
                    """.formatted(epc);
        }

        log.info("Testing RFID format {} to {} via {}", format, request.getPrinterAddress(), request.getPrintMethod());

        PrinterService.PrintResult result = printerService.sendToPrinter(
            zpl,
            request.getPrintMethod(),
            request.getPrinterAddress()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("format", format);
        response.put("formatDescription", formatDesc);
        response.put("epc", epc);
        response.put("zpl", zpl);

        return result.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
