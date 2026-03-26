package com.mirror.product.controller.label;

import com.mirror.product.dto.label.*;
import com.mirror.product.entity.label.RFIDScanLog;
import com.mirror.product.enums.RFIDTagStatus;
import com.mirror.product.service.label.RFIDTagService;
import com.mirror.product.service.label.ZPLGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rfid")
@RequiredArgsConstructor
public class RFIDController {

    private final RFIDTagService rfidTagService;

    @PostMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('RFID_MANAGE')")
    public ResponseEntity<RFIDTagResponse> registerTag(
            @Valid @RequestBody RFIDTagRegisterRequest request) {
        RFIDTagResponse response = rfidTagService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tags/{epc}")
    public ResponseEntity<RFIDTagResponse> getTagByEPC(@PathVariable String epc) {
        RFIDTagResponse response = rfidTagService.getByEPC(epc);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tags/{epc}/product")
    public ResponseEntity<RFIDTagResponse.ProductSummary> getProductByEPC(
            @PathVariable String epc) {
        RFIDTagResponse.ProductSummary product = rfidTagService.getProductByEPC(epc);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/scan")
    public ResponseEntity<RFIDScanResponse> recordScan(
            @Valid @RequestBody RFIDScanRequest request) {
        RFIDScanResponse response = rfidTagService.recordScan(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Scan by barcode - lookup product directly by barcode
     * For PDA devices scanning barcode instead of RFID
     */
    @GetMapping("/scan-barcode/{barcode}")
    public ResponseEntity<RFIDScanResponse> scanByBarcode(@PathVariable String barcode) {
        RFIDScanResponse response = rfidTagService.scanByBarcode(barcode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}/tags")
    public ResponseEntity<List<RFIDTagResponse>> getTagsByProduct(
            @PathVariable String productId) {
        List<RFIDTagResponse> tags = rfidTagService.getByProductId(productId);
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/tags/{epc}/void")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('RFID_MANAGE')")
    public ResponseEntity<Void> voidTag(
            @PathVariable String epc,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        rfidTagService.voidTag(epc, reason, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tags/{epc}/scans")
    public ResponseEntity<Page<RFIDScanLog>> getScanHistory(
            @PathVariable String epc,
            @PageableDefault(size = 20, sort = "scannedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RFIDScanLog> scans = rfidTagService.getScanHistory(epc, pageable);
        return ResponseEntity.ok(scans);
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('RFID_VIEW')")
    public ResponseEntity<Page<RFIDTagResponse>> getAllTags(
            @RequestParam(required = false) RFIDTagStatus status,
            @RequestParam(required = false) String productId,
            @PageableDefault(size = 20, sort = "encodedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RFIDTagResponse> tags = rfidTagService.getAll(status, productId, pageable);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/generate-epc")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> generateEPC(
            @RequestParam String productId,
            @RequestParam(required = false) String sku) {
        String epc = rfidTagService.generateEPC(productId, sku);
        return ResponseEntity.ok(Map.of("epc", epc));
    }

    @GetMapping("/decode-epc/{epc}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ZPLGeneratorService.EPCData> decodeEPC(
            @PathVariable String epc) {
        ZPLGeneratorService.EPCData data = rfidTagService.decodeEPC(epc);
        return ResponseEntity.ok(data);
    }

    /**
     * Check available scan data (for testing/debugging)
     * Returns sample products with barcodes and registered RFID tags
     */
    @GetMapping("/check-scan-data")
    public ResponseEntity<Map<String, Object>> checkScanData() {
        Map<String, Object> result = rfidTagService.checkScanData();
        return ResponseEntity.ok(result);
    }

    /**
     * Add test product with barcode (for testing only)
     */
    @PostMapping("/add-test-product")
    public ResponseEntity<Map<String, Object>> addTestProduct(
            @RequestParam String barcode,
            @RequestParam(defaultValue = "Test Product") String name) {
        Map<String, Object> result = rfidTagService.addTestProduct(barcode, name);
        return ResponseEntity.ok(result);
    }

    /**
     * Update product price by barcode (for testing only)
     */
    @PostMapping("/update-product-price")
    public ResponseEntity<Map<String, Object>> updateProductPrice(
            @RequestParam String barcode,
            @RequestParam java.math.BigDecimal price) {
        Map<String, Object> result = rfidTagService.updateProductPrice(barcode, price);
        return ResponseEntity.ok(result);
    }
}
