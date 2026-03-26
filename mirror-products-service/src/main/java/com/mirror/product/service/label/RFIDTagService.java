package com.mirror.product.service.label;

import com.mirror.product.dto.label.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.label.RFIDScanLog;
import com.mirror.product.entity.label.RFIDTag;
import com.mirror.product.enums.RFIDTagStatus;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.label.RFIDScanLogRepository;
import com.mirror.product.repository.label.RFIDTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RFIDTagService {

    private final RFIDTagRepository tagRepository;
    private final RFIDScanLogRepository scanLogRepository;
    private final MirrorProductRepository productRepository;
    private final ZPLGeneratorService zplGenerator;

    /**
     * Register a new RFID tag
     */
    @Transactional
    public RFIDTagResponse register(RFIDTagRegisterRequest request) {
        // Check if EPC already exists
        if (tagRepository.existsByEpc(request.getEpc())) {
            throw new IllegalStateException("RFID tag with EPC already exists: " + request.getEpc());
        }

        // Validate product
        MirrorProduct product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        RFIDTag tag = RFIDTag.builder()
            .epc(request.getEpc().toUpperCase())
            .productId(request.getProductId())
            .printJobId(request.getPrintJobId())
            .printJobItemId(request.getPrintJobItemId())
            .status(RFIDTagStatus.ACTIVE)
            .scanCount(0)
            .encodedAt(Instant.now())
            .metadata(request.getMetadata())
            .build();

        tag = tagRepository.save(tag);
        tag.setProduct(product);

        log.info("Registered RFID tag: {} for product {}", tag.getEpc(), request.getProductId());
        return RFIDTagResponse.fromEntity(tag);
    }

    /**
     * Get tag by EPC
     */
    public RFIDTagResponse getByEPC(String epc) {
        RFIDTag tag = tagRepository.findByEpcAndIsDeletedFalse(epc.toUpperCase())
            .orElseThrow(() -> new RuntimeException("RFID tag not found: " + epc));
        return RFIDTagResponse.fromEntity(tag);
    }

    /**
     * Get product by EPC
     */
    public RFIDTagResponse.ProductSummary getProductByEPC(String epc) {
        RFIDTag tag = tagRepository.findByEpcAndIsDeletedFalse(epc.toUpperCase())
            .orElseThrow(() -> new RuntimeException("RFID tag not found: " + epc));

        MirrorProduct product = productRepository.findById(tag.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));

        return RFIDTagResponse.ProductSummary.builder()
            .id(product.getId())
            .name(product.getItemName())
            .sku(product.getSkuCode())
            .barcode(product.getBarcode())
            .price(product.getPrice())
            .imageUrl(product.getImageUrl())
            .build();
    }

    /**
     * Record a scan event
     */
    @Transactional
    public RFIDScanResponse recordScan(RFIDScanRequest request) {
        String epc = request.getEpc() != null ? request.getEpc().toUpperCase() : null;

        // Create scan log first
        RFIDScanLog scanLog = RFIDScanLog.builder()
            .epc(epc)
            .deviceId(request.getDeviceId())
            .deviceName(request.getDeviceName())
            .deviceType(request.getDeviceType())
            .location(request.getLocation())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .userId(request.getUserId())
            .sessionId(request.getSessionId())
            .scanType(request.getScanType())
            .scannedAt(Instant.now())
            .isKnownTag(false)
            .build();

        // Try to find existing tag
        RFIDTag tag = null;
        MirrorProduct product = null;

        if (epc != null) {
            tag = tagRepository.findByEpcAndIsDeletedFalse(epc).orElse(null);
        }

        if (tag != null) {
            // Update tag scan info
            tag.recordScan(request.getDeviceId(), request.getLocation());
            tagRepository.save(tag);

            // Update scan log
            scanLog.setTagId(tag.getId());
            scanLog.setProductId(tag.getProductId());
            scanLog.setIsKnownTag(true);

            product = productRepository.findById(tag.getProductId()).orElse(null);
        }

        scanLog = scanLogRepository.save(scanLog);

        // Build response
        RFIDScanResponse.RFIDScanResponseBuilder response = RFIDScanResponse.builder()
            .success(true)
            .scanId(scanLog.getId())
            .tagFound(tag != null);

        if (tag != null) {
            response.tagInfo(RFIDScanResponse.RFIDTagInfo.builder()
                .epc(tag.getEpc())
                .status(tag.getStatus().name())
                .scanCount(tag.getScanCount())
                .build());

            if (product != null) {
                response.product(RFIDTagResponse.ProductSummary.builder()
                    .id(product.getId())
                    .name(product.getItemName())
                    .sku(product.getSkuCode())
                    .barcode(product.getBarcode())
                    .price(product.getPrice())
                    .imageUrl(product.getImageUrl())
                    .build());
            }
        } else {
            response.message("Unknown RFID tag");
        }

        log.debug("Recorded scan for EPC: {}, found: {}", epc, tag != null);
        return response.build();
    }

    /**
     * Scan by barcode - lookup product directly
     */
    public RFIDScanResponse scanByBarcode(String barcode) {
        MirrorProduct product = productRepository.findByBarcode(barcode).orElse(null);

        RFIDScanResponse.RFIDScanResponseBuilder response = RFIDScanResponse.builder()
            .success(true)
            .tagFound(product != null);

        if (product != null) {
            response.product(RFIDTagResponse.ProductSummary.builder()
                .id(product.getId())
                .name(product.getItemName())
                .sku(product.getSkuCode())
                .barcode(product.getBarcode())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .build());
            response.message("Product found by barcode");
        } else {
            response.message("Product not found with barcode: " + barcode);
        }

        log.debug("Barcode scan: {}, found: {}", barcode, product != null);
        return response.build();
    }

    /**
     * Get tags by product ID
     */
    public List<RFIDTagResponse> getByProductId(String productId) {
        List<RFIDTag> tags = tagRepository.findByProductIdAndIsDeletedFalse(productId);
        return tags.stream()
            .map(RFIDTagResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Void a tag
     */
    @Transactional
    public void voidTag(String epc, String reason, String userId) {
        RFIDTag tag = tagRepository.findByEpcAndIsDeletedFalse(epc.toUpperCase())
            .orElseThrow(() -> new RuntimeException("RFID tag not found: " + epc));

        tag.voidTag(reason, userId);
        tagRepository.save(tag);

        log.info("Voided RFID tag: {} by user {}, reason: {}", epc, userId, reason);
    }

    /**
     * Get scan history for a tag
     */
    public Page<RFIDScanLog> getScanHistory(String epc, Pageable pageable) {
        return scanLogRepository.findByEpcOrderByScannedAtDesc(epc.toUpperCase(), pageable);
    }

    /**
     * Generate EPC for a product
     */
    public String generateEPC(String productId, String sku) {
        return zplGenerator.generateEPC(productId, sku);
    }

    /**
     * Decode EPC
     */
    public ZPLGeneratorService.EPCData decodeEPC(String epc) {
        return zplGenerator.decodeEPC(epc);
    }

    /**
     * Get all tags with filters
     */
    public Page<RFIDTagResponse> getAll(RFIDTagStatus status, String productId, Pageable pageable) {
        Page<RFIDTag> tags = tagRepository.findWithFilters(status, productId, pageable);
        return tags.map(RFIDTagResponse::fromEntity);
    }

    /**
     * Add test product with barcode (for testing)
     */
    @Transactional
    public Map<String, Object> addTestProduct(String barcode, String name) {
        Map<String, Object> result = new java.util.HashMap<>();

        // Check if barcode already exists
        if (productRepository.findByBarcode(barcode).isPresent()) {
            result.put("success", false);
            result.put("message", "Barcode already exists: " + barcode);
            return result;
        }

        // Create new product
        MirrorProduct product = new MirrorProduct();
        product.setItemName(name);
        product.setSkuCode("TEST-" + barcode);
        product.setBarcode(barcode);
        product.setCategory("Test");
        product.setDescription("Test product for barcode scanning");

        product = productRepository.save(product);

        result.put("success", true);
        result.put("message", "Product created successfully");
        result.put("product", Map.of(
            "id", product.getId(),
            "name", product.getItemName(),
            "barcode", product.getBarcode(),
            "sku", product.getSkuCode()
        ));

        return result;
    }

    /**
     * Check available scan data for testing
     */
    public Map<String, Object> checkScanData() {
        Map<String, Object> result = new java.util.HashMap<>();

        // Get products with barcodes (limit 5)
        List<MirrorProduct> productsWithBarcode = productRepository.findAll().stream()
            .filter(p -> p.getBarcode() != null && !p.getBarcode().isEmpty())
            .limit(5)
            .collect(Collectors.toList());

        List<Map<String, Object>> barcodeList = productsWithBarcode.stream()
            .map(p -> {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getItemName());
                item.put("barcode", p.getBarcode());
                item.put("sku", p.getSkuCode());
                return item;
            })
            .collect(Collectors.toList());

        // Get registered RFID tags (limit 5)
        List<RFIDTag> rfidTags = tagRepository.findAll().stream()
            .filter(t -> !t.getIsDeleted())
            .limit(5)
            .collect(Collectors.toList());

        List<Map<String, Object>> rfidList = rfidTags.stream()
            .map(t -> {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("epc", t.getEpc());
                item.put("productId", t.getProductId());
                item.put("status", t.getStatus());
                item.put("scanCount", t.getScanCount());
                return item;
            })
            .collect(Collectors.toList());

        result.put("productsWithBarcode", barcodeList);
        result.put("productsWithBarcodeCount", barcodeList.size());
        result.put("registeredRFIDTags", rfidList);
        result.put("registeredRFIDTagsCount", rfidList.size());
        result.put("message", barcodeList.isEmpty() && rfidList.isEmpty()
            ? "Không có data để test. Cần thêm products với barcode hoặc đăng ký RFID tags."
            : "Có data để test scan.");

        return result;
    }

    /**
     * Update product price by barcode (for testing)
     */
    @Transactional
    public Map<String, Object> updateProductPrice(String barcode, java.math.BigDecimal price) {
        Map<String, Object> result = new java.util.HashMap<>();

        MirrorProduct product = productRepository.findByBarcode(barcode).orElse(null);
        if (product == null) {
            result.put("success", false);
            result.put("message", "Product not found with barcode: " + barcode);
            return result;
        }

        product.setPrice(price);
        product = productRepository.save(product);

        result.put("success", true);
        result.put("message", "Price updated successfully");
        result.put("product", Map.of(
            "id", product.getId(),
            "name", product.getItemName(),
            "barcode", product.getBarcode(),
            "price", product.getPrice()
        ));

        return result;
    }
}
