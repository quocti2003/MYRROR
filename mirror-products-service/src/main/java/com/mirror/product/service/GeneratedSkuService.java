package com.mirror.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mirror.product.client.misa.MisaAmisApiClient;
import com.mirror.product.dto.sku.GeneratedSkuListResponse;
import com.mirror.product.dto.sku.GeneratedSkuResponse;
import com.mirror.product.dto.sku.JewelrySkuRequest;
import com.mirror.product.dto.sku.PackagingSkuRequest;
import com.mirror.product.dto.sku.SkuGenerationResult;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.repository.MirrorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service for recording generated SKUs.
 *
 * New model (simplified):
 * - skuCode = unique barcode (e.g., MIR2512301234567) - primary identifier
 * - descriptiveCode = product specs (e.g., RNG-18KWG-W-2.09-LG-RD-1.29-N) - for display/search
 * - barcode = same as skuCode (kept for compatibility)
 * - Each product is unique (no more serialized units or product_units table)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratedSkuService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> SORTABLE_FIELDS = Set.of(
        "createdAt",
        "updatedAt",
        "skuCode",
        "itemName"
    );

    private final MirrorProductRepository mirrorProductRepository;
    private final BarcodeGenerationService barcodeGenerationService;
    private final MisaAmisApiClient amisApiClient;
    private final MisaOrderIntegrationService misaOrderIntegrationService;

    /**
     * Record a jewelry SKU with the new model:
     * - Barcode becomes the SKU code (unique identifier)
     * - Descriptive code (RNG-18KWG-...) stored in descriptive_code field
     */
    @Transactional
    public GeneratedSkuResponse recordJewelrySku(
            SkuGenerationResult result,
            JewelrySkuRequest request) {

        // itemName is now required, use it directly
        String itemName = request.getItemName();

        String description = buildDescription(
            "Jewelry SKU",
            request.getPrefix(),
            itemName
        );

        return persistGeneratedJewelrySku(
            result.getCode(),  // This is the descriptive code (RNG-18KWG-...)
            request.getPrefix(),
            itemName,
            description,
            request
        );
    }

    /**
     * Record a packaging SKU with the new model:
     * - Barcode becomes the SKU code (unique identifier)
     * - Descriptive code stored in descriptive_code field
     */
    @Transactional
    public GeneratedSkuResponse recordPackagingSku(
            SkuGenerationResult result,
            PackagingSkuRequest request) {

        // itemName is now required, use it directly
        String itemName = request.getItemName();

        String description = buildDescription(
            "Packaging SKU",
            request.getPrefix(),
            itemName
        );

        return persistGeneratedPackagingSku(
            result.getCode(),  // This is the descriptive code
            request.getPrefix(),
            itemName,
            description,
            request
        );
    }

    public Page<GeneratedSkuResponse> getGeneratedSkus(
            Integer page,
            Integer size,
            String sortBy,
            Sort.Direction direction) {

        int safePage = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int requestedSize = size != null ? size : DEFAULT_SIZE;
        int safeSize = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);

        String sortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        Sort sort = Sort.by(direction != null ? direction : Sort.Direction.DESC, sortField);

        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Page<MirrorProduct> pageResult = mirrorProductRepository.findAll(pageable);
        log.debug("Fetched {} generated SKUs (page {}, size {})", pageResult.getNumberOfElements(), safePage, safeSize);

        return pageResult.map(this::mapToResponse);
    }

    public GeneratedSkuListResponse toListResponse(Page<GeneratedSkuResponse> page) {
        return GeneratedSkuListResponse.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .codes(page.getContent())
            .build();
    }

    /**
     * Persist jewelry SKU with new model:
     * - Generate unique barcode to use as SKU code
     * - Store the descriptive code (RNG-18KWG-...) in descriptive_code field
     */
    private GeneratedSkuResponse persistGeneratedJewelrySku(
            String descriptiveCode,
            String prefix,
            String itemName,
            String description,
            JewelrySkuRequest request) {

        // Generate unique barcode - this becomes the SKU code
        String barcode = barcodeGenerationService.generateUniqueBarcode();

        // Check if a product with this barcode already exists (unlikely but safe)
        MirrorProduct product = mirrorProductRepository.findBySkuCode(barcode)
            .orElseGet(MirrorProduct::new);

        boolean isNewRecord = product.getId() == null;

        // Set the barcode as SKU code (primary identifier)
        product.setSkuCode(barcode);
        product.setBarcode(barcode);  // Keep barcode field for compatibility

        // Store the descriptive code (RNG-18KWG-W-2.09-LG-RD-1.29-N)
        product.setDescriptiveCode(descriptiveCode);

        if (StringUtils.hasText(prefix) && (isNewRecord || !StringUtils.hasText(product.getCategory()))) {
            product.setCategory(prefix);
        }

        if (StringUtils.hasText(itemName) && (isNewRecord || !StringUtils.hasText(product.getItemName()))) {
            product.setItemName(itemName);
        }

        if (StringUtils.hasText(description) && (isNewRecord || !StringUtils.hasText(product.getDescription()))) {
            product.setDescription(description);
        }

        // Set jewelry-specific fields from request
        if (StringUtils.hasText(request.getMaterial())) {
            product.setMetalType(request.getMaterial());
        }

        if (StringUtils.hasText(request.getMaterialColor())) {
            product.setMaterialColor(request.getMaterialColor());
        }

        if (StringUtils.hasText(request.getMaterialWeight())) {
            product.setMaterialWeight(request.getMaterialWeight());
        }

        if (request.getIsCoated() != null) {
            product.setIsCoated(request.getIsCoated());
        }

        if (StringUtils.hasText(request.getCoatingMaterial())) {
            product.setCoatingMaterial(request.getCoatingMaterial());
        }

        if (StringUtils.hasText(request.getOrigin())) {
            product.setStoneOrigin(request.getOrigin());
        }

        if (StringUtils.hasText(request.getShape())) {
            product.setStoneShape(request.getShape());
        }

        if (StringUtils.hasText(request.getWeight())) {
            product.setStoneWeight(request.getWeight());
        }

        if (StringUtils.hasText(request.getSideStones())) {
            product.setSideStones(request.getSideStones());
        }

        if (StringUtils.hasText(request.getCountryOfOrigin())) {
            product.setCountryOfOrigin(request.getCountryOfOrigin());
        }

        // Set location if provided
        if (StringUtils.hasText(request.getUnitLocation())) {
            product.setLocation(request.getUnitLocation());
        }

        // Set cost and price if provided
        if (request.getUnitCostPrice() != null) {
            product.setCost(request.getUnitCostPrice());
        }

        if (request.getUnitSalePrice() != null) {
            product.setPrice(request.getUnitSalePrice());
        }

        // Stock quantity is not set by default - must be explicitly added later

        // Set default draft status if new
        if (isNewRecord && product.getStatus() == null) {
            product.setStatus(ProductStatus.DRAFT);
        }

        MirrorProduct saved = mirrorProductRepository.save(product);
        log.info("Recorded jewelry product (skuCode={}, descriptiveCode={}, barcode={})",
                 saved.getSkuCode(), saved.getDescriptiveCode(), saved.getBarcode());

        pushToMisaAsync(saved);

        return mapToResponse(saved);
    }

    /**
     * Persist packaging SKU with new model:
     * - Generate unique barcode to use as SKU code
     * - Store the descriptive code in descriptive_code field
     */
    private GeneratedSkuResponse persistGeneratedPackagingSku(
            String descriptiveCode,
            String prefix,
            String itemName,
            String description,
            PackagingSkuRequest request) {

        // Generate unique barcode - this becomes the SKU code
        String barcode = barcodeGenerationService.generateUniqueBarcode();

        MirrorProduct product = mirrorProductRepository.findBySkuCode(barcode)
            .orElseGet(MirrorProduct::new);

        boolean isNewRecord = product.getId() == null;

        // Set the barcode as SKU code (primary identifier)
        product.setSkuCode(barcode);
        product.setBarcode(barcode);  // Keep barcode field for compatibility

        // Store the descriptive code
        product.setDescriptiveCode(descriptiveCode);

        if (StringUtils.hasText(prefix) && (isNewRecord || !StringUtils.hasText(product.getCategory()))) {
            product.setCategory(prefix);
        }

        if (StringUtils.hasText(itemName) && (isNewRecord || !StringUtils.hasText(product.getItemName()))) {
            product.setItemName(itemName);
        }

        if (StringUtils.hasText(description) && (isNewRecord || !StringUtils.hasText(product.getDescription()))) {
            product.setDescription(description);
        }

        // Set stock quantity only if explicitly provided
        if (request.getStockQuantity() != null && request.getStockQuantity() > 0) {
            product.setStockQuantity(request.getStockQuantity());
        }

        // Set location if provided
        if (StringUtils.hasText(request.getStockLocation())) {
            product.setLocation(request.getStockLocation());
        }

        // Set country of origin
        if (StringUtils.hasText(request.getCountryOfOrigin())) {
            product.setCountryOfOrigin(request.getCountryOfOrigin());
        }

        // Set default draft status if new
        if (isNewRecord && product.getStatus() == null) {
            product.setStatus(ProductStatus.DRAFT);
        }

        MirrorProduct saved = mirrorProductRepository.save(product);
        log.info("Recorded packaging product (skuCode={}, descriptiveCode={}, barcode={}, stockQuantity={})",
                 saved.getSkuCode(), saved.getDescriptiveCode(), saved.getBarcode(), saved.getStockQuantity());

        pushToMisaAsync(saved);

        return mapToResponse(saved);
    }

    /**
     * Push a newly created product to MISA AMIS asynchronously.
     * Non-blocking: failures are logged but do not affect SKU creation.
     */
    private void pushToMisaAsync(MirrorProduct product) {
        if (Boolean.TRUE.equals(product.getMisaSynced())) {
            log.debug("Product {} already synced to MISA, skipping push", product.getSkuCode());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Pushing new SKU to MISA AMIS: skuCode={}, descriptiveCode={}",
                        product.getSkuCode(), product.getDescriptiveCode());

                Map<String, Object> skuItem = misaOrderIntegrationService.buildSkuDictionaryPayload(product);

                JsonNode response = amisApiClient.saveDictionary(3, List.of((Object) skuItem)).block();

                if (response != null && response.has("Success") && response.get("Success").asBoolean()) {
                    log.info("MISA SKU push successful for product: {}", product.getSkuCode());
                    product.setMisaSynced(true);
                    product.setMisaSyncDate(LocalDateTime.now());
                    // Use barcode (skuCode) as misaItemCode to match what we send to MISA
                    product.setMisaItemCode(product.getSkuCode());
                    mirrorProductRepository.save(product);
                } else {
                    String errorMsg = response != null && response.has("ErrorMessage")
                            ? response.get("ErrorMessage").asText()
                            : "Unknown MISA error";
                    log.warn("MISA SKU push failed for product {}: {}", product.getSkuCode(), errorMsg);
                }
            } catch (Exception e) {
                log.error("Failed to push SKU to MISA for product: {}", product.getSkuCode(), e);
            }
        });
    }

    private String buildDescription(String prefix, String category, String itemName) {
        StringBuilder builder = new StringBuilder(prefix);

        if (StringUtils.hasText(category)) {
            builder.append(" | Prefix: ").append(category);
        }

        if (StringUtils.hasText(itemName)) {
            builder.append(" | Details: ").append(itemName);
        }

        builder.append(" | Source: SKU Generator");
        return builder.toString();
    }

    private GeneratedSkuResponse mapToResponse(MirrorProduct product) {
        return GeneratedSkuResponse.builder()
            .skuCode(product.getSkuCode())
            .descriptiveCode(product.getDescriptiveCode())  // New field
            .barcode(product.getBarcode())
            .misaItemCode(product.getMisaItemCode())
            .itemName(product.getItemName())
            .category(product.getCategory())
            .description(product.getDescription())
            .price(product.getPrice())
            .currentQuantity(product.getStockQuantity())
            .minQuantity(product.getMinStockLevel())
            .location(product.getLocation())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}
