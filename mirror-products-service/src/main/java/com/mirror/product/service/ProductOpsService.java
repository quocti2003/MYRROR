package com.mirror.product.service;

import com.mirror.product.dto.ProductFulfillmentRequest;
import com.mirror.product.dto.ProductRequest;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.dto.productops.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.misa.MisaInventoryItem;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.repository.misa.MisaInventoryItemRepository;
import com.mirror.product.repository.MirrorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ProductOpsService - Orchestrates Product Operations Dashboard workflow
 *
 * Delegates to existing services:
 * - ProductService: Product CRUD and workflow
 * - MisaInventoryItemRepository: MISA sync status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOpsService {

    private final MirrorProductRepository mirrorProductRepository;
    private final ProductService productService;
    private final MisaInventoryItemRepository misaInventoryItemRepository;

    // ==================== DASHBOARD & OVERVIEW ====================

    public List<ProductWorkflowResponse> getAllProductsWithWorkflow(String status, String search) {
        log.info("Getting all products with workflow - status: {}, search: {}", status, search);

        List<MirrorProduct> products;

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            products = getProductsByWorkflowStatus(status);
        } else {
            products = mirrorProductRepository.findAll();
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getItemName().toLowerCase().contains(searchLower) ||
                                (p.getSkuCode() != null && p.getSkuCode().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        return products.stream()
                .map(this::mapToWorkflowResponse)
                .collect(Collectors.toList());
    }

    public WorkflowSummaryResponse getWorkflowSummary() {
        long draft = mirrorProductRepository.countByStatus(ProductStatus.DRAFT);
        long inProgress = countInProgressProducts();
        long ready = mirrorProductRepository.countByStatus(ProductStatus.READY_FOR_RELEASE);
        long published = mirrorProductRepository.countByStatus(ProductStatus.PUBLISHED);

        return WorkflowSummaryResponse.builder()
                .draft((int) draft)
                .inProgress((int) inProgress)
                .ready((int) ready)
                .published((int) published)
                .total((int) (draft + inProgress + ready + published))
                .build();
    }

    public Optional<ProductWorkflowResponse> getProductWorkflow(String id) {
        return mirrorProductRepository.findById(id)
                .map(this::mapToWorkflowResponse);
    }

    // ==================== DRAFT PRODUCT CREATION (Delegates to ProductService) ====================

    @Transactional
    public ProductWorkflowResponse createDraftProduct(CreateDraftRequest request) {
        log.info("Creating draft product for SKU: {}", request.getInternalSKU());

        ProductRequest productRequest = new ProductRequest();
        productRequest.setName(request.getName());
        productRequest.setSkuCode(request.getInternalSKU());
        productRequest.setPrice(java.math.BigDecimal.ZERO); // Default price
        productRequest.setCurrency("VND"); // Default currency

        ProductResponse response = productService.createProduct(productRequest);

        // Return as ProductWorkflowResponse
        return mirrorProductRepository.findById(response.getId())
                .map(this::mapToWorkflowResponse)
                .orElseThrow(() -> new RuntimeException("Product not found after creation"));
    }

    // ==================== PRODUCT FULFILLMENT (Delegates to ProductService) ====================

    @Transactional
    public ProductResponse fulfillProduct(String productId, ProductFulfillmentRequest request) {
        log.info("Fulfilling product: {}", productId);
        return productService.fulfillProduct(productId, request);
    }

    @Transactional
    public Optional<ProductWorkflowResponse> updateProductFulfillment(
            String productId, com.mirror.product.dto.productops.ProductFulfillmentRequest request) {
        log.info("Updating product fulfillment: {}", productId);

        // Map productops request to standard request
        ProductFulfillmentRequest stdRequest = new ProductFulfillmentRequest();
        // TODO: Map fields as needed

        productService.fulfillProduct(productId, stdRequest);
        return mirrorProductRepository.findById(productId)
                .map(this::mapToWorkflowResponse);
    }

    // ==================== MISA SYNC STATUS CHECKING ====================

    public MISAProductResponse getMISAProductBySkuCode(String skuCode) {
        log.info("Checking MISA sync status for SKU: {}", skuCode);

        Optional<MisaInventoryItem> misaItem = misaInventoryItemRepository.findByInventoryItemCode(skuCode);

        if (misaItem.isEmpty()) {
            // Not synced yet
            return MISAProductResponse.builder()
                    .internalSKU(skuCode)
                    .misaSKU(null)
                    .misaStatus("PENDING")
                    .build();
        }

        MisaInventoryItem item = misaItem.get();
        return MISAProductResponse.builder()
                .id(item.getId().toString())
                .internalSKU(skuCode)
                .misaSKU(item.getInventoryItemCode())
                .name(item.getInventoryItemName())
                .thumbnail(item.getImageUrl())
                .category(item.getInventoryItemCategoryName())
                .misaStatus(mapMisaSyncStatus(item.getSyncStatus()))
                .createdAt(item.getCreatedAt() != null ?
                          item.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .misaSyncedAt(item.getLastSyncDate())
                .misaLastAttempt(item.getLastSyncDate())
                .misaAttempts(1) // TODO: Track actual attempts if needed
                .misaError(item.getSyncErrorMessage())
                .build();
    }

    public List<MISAProductResponse> getPendingMISAProducts() {
        log.info("Getting products pending MISA sync");

        // Products that are READY_FOR_RELEASE but not yet in MISA
        List<MirrorProduct> readyProducts = mirrorProductRepository.findByStatus(ProductStatus.READY_FOR_RELEASE);

        return readyProducts.stream()
                .map(product -> {
                    Optional<MisaInventoryItem> misaItem =
                        misaInventoryItemRepository.findByInventoryItemCode(product.getSkuCode());

                    return MISAProductResponse.builder()
                            .id(product.getId().toString())
                            .internalSKU(product.getSkuCode())
                            .name(product.getItemName())
                            .thumbnail(product.getImageUrl())
                            .category(product.getCategory())
                            .misaStatus(misaItem.isPresent() ?
                                      mapMisaSyncStatus(misaItem.get().getSyncStatus()) : "PENDING")
                            .createdAt(product.getCreatedAt() != null ?
                                      product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<MISAProductResponse> getMISAProducts(String skuPrefix) {
        log.info("Getting MISA products with prefix: {}", skuPrefix);

        // Search MISA inventory items
        List<MisaInventoryItem> misaItems = skuPrefix != null ?
                misaInventoryItemRepository.searchByNameOrCode(skuPrefix) :
                misaInventoryItemRepository.findAll();

        return misaItems.stream()
                .map(item -> MISAProductResponse.builder()
                        .id(item.getId().toString())
                        .internalSKU(item.getInventoryItemCode())
                        .misaSKU(item.getInventoryItemCode())
                        .name(item.getInventoryItemName())
                        .thumbnail(item.getImageUrl())
                        .category(item.getInventoryItemCategoryName())
                        .misaStatus(mapMisaSyncStatus(item.getSyncStatus()))
                        .createdAt(item.getCreatedAt() != null ?
                                  item.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                        .misaSyncedAt(item.getLastSyncDate())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public MISARequestResponse requestMISASKU(String productId) {
        log.info("Requesting MISA SKU for product: {}", productId);

        // This is a placeholder - actual MISA request happens via Excel upload
        return MISARequestResponse.builder()
                .productId(productId)
                .message("MISA SKU request prepared. Please export to Excel and upload to MISA.")
                .status("PENDING")
                .requestedAt(Instant.now())
                .attemptNumber(1)
                .build();
    }

    @Transactional
    public MISARequestResponse retryMISASync(String productId) {
        log.info("Retrying MISA sync for product: {}", productId);

        return MISARequestResponse.builder()
                .productId(productId)
                .message("MISA sync retry initiated")
                .status("PENDING")
                .requestedAt(Instant.now())
                .attemptNumber(2)
                .build();
    }

    @Transactional
    public Optional<ProductWorkflowResponse> updateMISASKU(String productId, String misaSKU) {
        log.info("Updating MISA SKU for product {}: {}", productId, misaSKU);

        return mirrorProductRepository.findById(productId)
                .map(product -> {
                    // TODO: Add MISA SKU field to MirrorProduct entity if needed
                    mirrorProductRepository.save(product);
                    return mapToWorkflowResponse(product);
                });
    }

    // ==================== MARK READY & PUBLISH (Delegates to ProductService) ====================

    @Transactional
    public Optional<ProductWorkflowResponse> markProductReady(String productId) {
        log.info("Marking product ready for release: {}", productId);
        productService.markReadyForRelease(productId);
        return mirrorProductRepository.findById(productId)
                .map(this::mapToWorkflowResponse);
    }

    @Transactional
    public Optional<ProductWorkflowResponse> publishProduct(String productId) {
        log.info("Publishing product: {}", productId);
        productService.publishProduct(productId);
        return mirrorProductRepository.findById(productId)
                .map(this::mapToWorkflowResponse);
    }

    // ==================== WORKFLOW STEP UPDATES ====================

    @Transactional
    public Optional<ProductWorkflowResponse> updateWorkflowStep(String id, WorkflowStepUpdateRequest request) {
        log.info("Updating workflow step {} for product {}: {}", request.getStepId(), id, request.getStatus());

        return mirrorProductRepository.findById(id)
                .map(product -> {
                    // TODO: Implement step tracking in database
                    // For now, update product status based on step completion
                    boolean completed = "complete".equalsIgnoreCase(request.getStatus());
                    updateProductStatusBasedOnStep(product, request.getStepId(), completed);
                    mirrorProductRepository.save(product);
                    return mapToWorkflowResponse(product);
                });
    }

    public List<WorkflowHistoryResponse> getProductWorkflowHistory(String id) {
        log.info("Getting workflow history for product: {}", id);
        // TODO: Implement workflow history tracking
        return Collections.emptyList();
    }

    public List<MISAHistoryResponse> getProductMISAHistory(String id) {
        log.info("Getting MISA history for product: {}", id);
        // TODO: Implement MISA history tracking
        return Collections.emptyList();
    }

    // ==================== ADDITIONAL DASHBOARD QUERIES ====================

    public List<ProductWorkflowResponse> getReadyProducts() {
        log.info("Getting ready products");
        List<MirrorProduct> products = mirrorProductRepository.findByStatus(ProductStatus.READY_FOR_RELEASE);
        return products.stream()
                .map(this::mapToWorkflowResponse)
                .collect(Collectors.toList());
    }

    public List<ProductWorkflowResponse> getPublishedProducts() {
        log.info("Getting published products");
        List<MirrorProduct> products = mirrorProductRepository.findByStatus(ProductStatus.PUBLISHED);
        return products.stream()
                .map(this::mapToWorkflowResponse)
                .collect(Collectors.toList());
    }

    public List<WorkflowHistoryResponse> getWorkflowHistory(String id) {
        return getProductWorkflowHistory(id);
    }

    public List<MISAHistoryResponse> getMISAHistory(String id) {
        return getProductMISAHistory(id);
    }

    public Optional<ProductWorkflowResponse> unpublishProduct(String id) {
        log.info("Unpublishing product: {}", id);
        return mirrorProductRepository.findById(id)
                .map(product -> {
                    product.setStatus(ProductStatus.READY_FOR_RELEASE);
                    mirrorProductRepository.save(product);
                    return mapToWorkflowResponse(product);
                });
    }

    // ==================== HELPER METHODS ====================

    private List<MirrorProduct> getProductsByWorkflowStatus(String status) {
        return switch (status.toUpperCase()) {
            case "DRAFT" -> mirrorProductRepository.findByStatus(ProductStatus.DRAFT);
            case "IN_PROGRESS" -> getInProgressProducts();
            case "READY" -> mirrorProductRepository.findByStatus(ProductStatus.READY_FOR_RELEASE);
            case "PUBLISHED" -> mirrorProductRepository.findByStatus(ProductStatus.PUBLISHED);
            default -> mirrorProductRepository.findAll();
        };
    }

    private List<MirrorProduct> getInProgressProducts() {
        // Products that are DRAFT but have started fulfillment
        List<MirrorProduct> draftProducts = mirrorProductRepository.findByStatus(ProductStatus.DRAFT);
        return draftProducts.stream()
                .filter(p -> p.getFulfillmentCompletedAt() == null &&
                            (p.getImageUrls() != null || p.getDescription() != null))
                .collect(Collectors.toList());
    }

    private long countInProgressProducts() {
        return getInProgressProducts().size();
    }

    private ProductWorkflowResponse mapToWorkflowResponse(MirrorProduct product) {
        // Check MISA sync status
        Optional<MisaInventoryItem> misaItem =
            misaInventoryItemRepository.findByInventoryItemCode(product.getSkuCode());

        String misaStatus = misaItem.map(item -> mapMisaSyncStatus(item.getSyncStatus()))
                .orElse("PENDING");

        // Build checklist
        ProductWorkflowResponse.ChecklistStatus checklist = ProductWorkflowResponse.ChecklistStatus.builder()
                .hasImages(product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                .hasDescription(product.getDescription() != null && !product.getDescription().isEmpty())
                .hasMISASKU(misaItem.isPresent())
                .hasSpecifications(false) // TODO: Check specifications
                .hasCategory(product.getCategory() != null)
                .hasModel3d((product.getModel3dId() != null && !product.getModel3dId().isEmpty()) || Boolean.TRUE.equals(product.getModel3dNotRequired()))
                .build();

        // Build workflow steps
        List<ProductWorkflowResponse.WorkflowStepDetail> steps = buildWorkflowSteps(product, misaItem.orElse(null));

        return ProductWorkflowResponse.builder()
                .id(product.getId().toString())
                .name(product.getItemName())
                .internalSKU(product.getSkuCode())
                .misaSKU(misaItem.map(MisaInventoryItem::getInventoryItemCode).orElse(null))
                .thumbnail(product.getImageUrl())
                .imageUrls(product.getImageUrls() != null ?
                          List.of(product.getImageUrls().split(",")) : Collections.emptyList())
                .shortDescription(product.getDescription())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .status(product.getStatus().name())
                .currentStep(determineCurrentStep(product, misaItem.orElse(null)))
                .steps(steps)
                .createdAt(product.getCreatedAt() != null ?
                          product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .publishedAt(product.getPublishedAt())
                .markedReadyAt(product.getFulfillmentCompletedAt())
                .misaStatus(misaStatus)
                .misaSyncedAt(misaItem.map(MisaInventoryItem::getLastSyncDate).orElse(null))
                .checklist(checklist)
                .build();
    }

    private List<ProductWorkflowResponse.WorkflowStepDetail> buildWorkflowSteps(
            MirrorProduct product, MisaInventoryItem misaItem) {

        List<ProductWorkflowResponse.WorkflowStepDetail> steps = new ArrayList<>();

        // Step 1: SKU Generated
        steps.add(ProductWorkflowResponse.WorkflowStepDetail.builder()
                .id(1)
                .status(product.getSkuCode() != null ? "complete" : "pending")
                .completedAt(product.getCreatedAt() != null ?
                           product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build());

        // Step 2: Draft Created
        steps.add(ProductWorkflowResponse.WorkflowStepDetail.builder()
                .id(2)
                .status(product.getId() != null ? "complete" : "pending")
                .completedAt(product.getCreatedAt() != null ?
                           product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build());

        // Step 3: Fulfillment (Images & Description)
        boolean fulfilled = product.getImageUrls() != null && product.getDescription() != null;
        steps.add(ProductWorkflowResponse.WorkflowStepDetail.builder()
                .id(3)
                .status(fulfilled ? "complete" : "pending")
                .completedAt(product.getFulfillmentCompletedAt() != null ?
                           product.getFulfillmentCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build());

        // Step 4: MISA SKU Synced
        boolean misaSynced = misaItem != null &&
                           misaItem.getSyncStatus() == MisaInventoryItem.SyncStatus.SYNCED;
        steps.add(ProductWorkflowResponse.WorkflowStepDetail.builder()
                .id(4)
                .status(misaSynced ? "complete" : "pending")
                .completedAt(misaItem != null && misaItem.getLastSyncDate() != null ?
                           misaItem.getLastSyncDate().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build());

        // Step 5: Marked Ready
        steps.add(ProductWorkflowResponse.WorkflowStepDetail.builder()
                .id(5)
                .status(product.getStatus() == ProductStatus.READY_FOR_RELEASE ||
                       product.getStatus() == ProductStatus.PUBLISHED ? "complete" : "pending")
                .completedAt(product.getFulfillmentCompletedAt() != null ?
                           product.getFulfillmentCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build());

        // Step 6: Published
        steps.add(ProductWorkflowResponse.WorkflowStepDetail.builder()
                .id(6)
                .status(product.getStatus() == ProductStatus.PUBLISHED ? "complete" : "pending")
                .completedAt(product.getPublishedAt() != null ?
                           product.getPublishedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build());

        return steps;
    }

    private Integer determineCurrentStep(MirrorProduct product, MisaInventoryItem misaItem) {
        if (product.getStatus() == ProductStatus.PUBLISHED) {
            return 6;
        }
        if (product.getStatus() == ProductStatus.READY_FOR_RELEASE) {
            return 5;
        }
        if (misaItem != null && misaItem.getSyncStatus() == MisaInventoryItem.SyncStatus.SYNCED) {
            return 4;
        }
        if (product.getFulfillmentCompletedAt() != null) {
            return 3;
        }
        if (product.getId() != null) {
            return 2;
        }
        return 1;
    }

    private String mapMisaSyncStatus(MisaInventoryItem.SyncStatus syncStatus) {
        return switch (syncStatus) {
            case SYNCED, UPDATED -> "SYNCED";
            case ERROR -> "FAILED";
            case PENDING -> "PENDING";
        };
    }

    private void updateProductStatusBasedOnStep(MirrorProduct product, Integer stepId, Boolean completed) {
        // Helper method to update product status when workflow steps change
        // This is a simplified version - you may need more complex logic
        if (completed && stepId == 5) {
            product.setStatus(ProductStatus.READY_FOR_RELEASE);
        } else if (completed && stepId == 6) {
            product.setStatus(ProductStatus.PUBLISHED);
        }
    }
}
