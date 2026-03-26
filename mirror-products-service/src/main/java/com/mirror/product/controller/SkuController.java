package com.mirror.product.controller;

import com.mirror.product.dto.SkuRequest;
import com.mirror.product.dto.SkuResponse;
import com.mirror.product.dto.sku.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.service.SkuService;
import com.mirror.product.service.SkuCodeService;
import com.mirror.product.service.GeneratedSkuService;
import com.mirror.product.service.SkuImportService;
import com.mirror.product.service.SkuSearchService;
import com.mirror.product.service.MisaTemplateExportService;
import com.mirror.product.mapper.SkuMapper;
import com.mirror.product.util.NumberValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/skus")
@RequiredArgsConstructor
@Slf4j
public class SkuController {

    private final SkuService skuService;
    private final SkuMapper skuMapper;
    private final SkuCodeService skuCodeService;
    private final GeneratedSkuService generatedSkuService;
    private final SkuImportService skuImportService;
    private final SkuSearchService skuSearchService;
    private final MisaTemplateExportService misaTemplateExportService;

    @GetMapping
    public ResponseEntity<List<SkuResponse>> getAllSkus() {
        List<MirrorProduct> products = skuService.findAllActive();
        List<SkuResponse> responses = products.stream()
                .map(skuMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{skuId}")
    public ResponseEntity<SkuResponse> getSkuById(@PathVariable String skuId) {
        return skuService.findActiveById(skuId)
                .map(skuMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{skuName}")
    public ResponseEntity<SkuResponse> getSkuByName(@PathVariable String skuName) {
        return skuService.findBySkuName(skuName)
                .map(skuMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSku(@Valid @RequestBody SkuRequest request) {
        try {
            MirrorProduct product = skuMapper.toEntity(request);
            MirrorProduct savedProduct = skuService.save(product);
            SkuResponse response = skuMapper.toResponse(savedProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating SKU");
        }
    }

    @PutMapping("/{skuId}")
    public ResponseEntity<?> updateSku(@PathVariable String skuId, @RequestBody SkuRequest request){
        try {
            MirrorProduct existingProduct = skuService.findActiveById(skuId)
                    .orElseThrow(() -> new RuntimeException("SKU not found"));
            skuMapper.updateEntityFromRequest(existingProduct, request);
            MirrorProduct updatedProduct = skuService.save(existingProduct);
            SkuResponse response = skuMapper.toResponse(updatedProduct);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating SKU");
        }
    }

    @DeleteMapping("/{skuId}")
    public ResponseEntity<?> deleteSku(@PathVariable String skuId) {
        try {
            skuService.softDeleteById(skuId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting SKU");
        }
    }

    @PatchMapping("/{skuId}/deactivate")
    public ResponseEntity<?> deactivateSku(@PathVariable String skuId) {
        try {
            skuService.deactivateById(skuId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deactivating SKU");
        }
    }

    @GetMapping("/exists/{skuName}")
    public ResponseEntity<Boolean> checkSkuExists(@PathVariable String skuName) {
        boolean exists = skuService.existsBySkuName(skuName);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        long count = skuService.countActive();
        return ResponseEntity.ok(count);
    }

    // ==================== SKU GENERATION ENDPOINTS ====================

    @PostMapping("/generate/jewelry")
    public ResponseEntity<SkuGenerationResponse> generateJewelrySku(
            @Valid @RequestBody JewelrySkuRequest request) {

        log.info("Generating jewelry SKU for prefix: {}", request.getPrefix());

        String processedMaterialWeight = NumberValidationUtil.validateAndRoundWeight(request.getMaterialWeight());

        SkuGenerationResult result = skuCodeService.generateJewelrySku(
            request.getPrefix(),
            request.getMaterial(),
            request.getMaterialColor(),
            processedMaterialWeight,
            request.getIsCoated(),
            request.getCoatingMaterial(),
            request.getOrigin(),
            request.getShape(),
            request.getWeight(),
            request.getSideStones(),
            request.getVariant()
        );

        request.setMaterialWeight(processedMaterialWeight);
        GeneratedSkuResponse savedProduct = generatedSkuService.recordJewelrySku(result, request);

        SkuGenerationResponse response = SkuGenerationResponse.builder()
            .code(result.getCode())
            .barcode(savedProduct.getBarcode())
            .description("Generated jewelry product")
            .length(result.getCode().length())
            .truncated(result.isTruncated())
            .productId(savedProduct.getSkuCode())
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate/packaging")
    public ResponseEntity<SkuGenerationResponse> generatePackagingSku(
            @Valid @RequestBody PackagingSkuRequest request) {

        log.info("Generating packaging SKU for prefix: {}, quantity: {}",
                 request.getPrefix(), request.getStockQuantity());

        SkuGenerationResult result = skuCodeService.generatePackagingSku(
            request.getPrefix(),
            request.getMaterial(),
            request.getSize(),
            request.getColor(),
            request.getType(),
            request.getFinish(),
            request.getNotes()
        );

        GeneratedSkuResponse savedProduct = generatedSkuService.recordPackagingSku(result, request);

        SkuGenerationResponse response = SkuGenerationResponse.builder()
            .code(result.getCode())
            .barcode(savedProduct.getBarcode())
            .description("Generated packaging product")
            .length(result.getCode().length())
            .truncated(result.isTruncated())
            .productId(savedProduct.getSkuCode())
            .build();

        return ResponseEntity.ok(response);
    }

    // ==================== BULK IMPORT ENDPOINTS ====================

    @PostMapping(value = "/import/jewelry", consumes = "multipart/form-data")
    public ResponseEntity<BulkSkuGenerationResponse> importJewelrySkus(
            @RequestPart("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                BulkSkuGenerationResponse.builder()
                    .totalRows(0)
                    .successCount(0)
                    .failureCount(0)
                    .generatedCodes(List.of())
                    .errors(List.of("File attachment is required"))
                    .build()
            );
        }

        BulkSkuGenerationResponse response = skuImportService.importJewelrySkus(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/import/packaging", consumes = "multipart/form-data")
    public ResponseEntity<BulkSkuGenerationResponse> importPackagingSkus(
            @RequestPart("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                BulkSkuGenerationResponse.builder()
                    .totalRows(0)
                    .successCount(0)
                    .failureCount(0)
                    .generatedCodes(List.of())
                    .errors(List.of("File attachment is required"))
                    .build()
            );
        }

        BulkSkuGenerationResponse response = skuImportService.importPackagingSkus(file);
        return ResponseEntity.ok(response);
    }

    // ==================== MISA EXPORT ENDPOINTS ====================

    @GetMapping("/export-misa")
    public ResponseEntity<byte[]> exportAllToMisa() {
        try {
            log.info("Exporting all generated SKUs to MISA template");

            byte[] excelFile = misaTemplateExportService.exportToMisaTemplate();
            String filename = misaTemplateExportService.generateExportFilename();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelFile.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);

        } catch (Exception e) {
            log.error("Error exporting to MISA template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/export-misa-by-ids")
    public ResponseEntity<byte[]> exportByIdsToMisa(@RequestBody List<String> productIds) {
        try {
            log.info("Exporting {} products to MISA template", productIds.size());

            byte[] excelFile = misaTemplateExportService.exportProductsByIds(productIds);
            String filename = misaTemplateExportService.generateExportFilename();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelFile.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);

        } catch (Exception e) {
            log.error("Error exporting selected products to MISA template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export-misa-by-category")
    public ResponseEntity<byte[]> exportByCategoryToMisa(@RequestParam String category) {
        try {
            log.info("Exporting products by category '{}' to MISA template", category);

            byte[] excelFile = misaTemplateExportService.exportProductsByCategory(category);
            String filename = misaTemplateExportService.generateExportFilename();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelFile.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);

        } catch (Exception e) {
            log.error("Error exporting category to MISA template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== GENERATED SKU LIST ====================

    @GetMapping("/generated")
    public ResponseEntity<GeneratedSkuListResponse> getGeneratedSkus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Page<GeneratedSkuResponse> skusPage = generatedSkuService.getGeneratedSkus(
            page,
            size,
            sortBy,
            direction
        );

        GeneratedSkuListResponse response = generatedSkuService.toListResponse(skusPage);
        return ResponseEntity.ok(response);
    }

    // ==================== SEARCH ====================

    @GetMapping("/search")
    public ResponseEntity<SkuSearchResponse> searchSkus(
            @RequestParam String q,
            @RequestParam(defaultValue = "0.3") double threshold,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Searching SKUs with query: '{}', threshold: {}", q, threshold);

        List<SkuSearchService.SearchResult> results = skuSearchService.searchSkus(q, threshold);

        if (results.size() > limit) {
            results = results.subList(0, limit);
        }

        List<SkuSearchResponse.SearchResultItem> items = results.stream()
            .map(result -> SkuSearchResponse.SearchResultItem.builder()
                .skuCode(result.getSkuCode())
                .itemName(result.getItemName())
                .description(result.getDescription())
                .category(result.getProduct().getCategory())
                .price(result.getProduct().getPrice() != null ?
                       result.getProduct().getPrice().doubleValue() : null)
                .relevance(result.getRelevance())
                .build())
            .collect(Collectors.toList());

        SkuSearchResponse response = SkuSearchResponse.builder()
            .query(q)
            .totalResults(items.size())
            .results(items)
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<SkuSearchResponse> searchSkusPost(
            @Valid @RequestBody SkuSearchRequest request) {

        return searchSkus(
            request.getQuery(),
            request.getThreshold(),
            request.getLimit()
        );
    }

    // ==================== TEMPLATES ====================

    @GetMapping("/templates/jewelry")
    public ResponseEntity<org.springframework.core.io.Resource> downloadJewelryTemplate() {
        try {
            org.springframework.core.io.ClassPathResource resource =
                new org.springframework.core.io.ClassPathResource("templates/jewelry_items.csv");

            if (!resource.exists()) {
                log.error("Jewelry template file not found");
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "jewelry_items_template.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading jewelry template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/templates/packaging")
    public ResponseEntity<org.springframework.core.io.Resource> downloadPackagingTemplate() {
        try {
            org.springframework.core.io.ClassPathResource resource =
                new org.springframework.core.io.ClassPathResource("templates/packaging_items.csv");

            if (!resource.exists()) {
                log.error("Packaging template file not found");
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "packaging_items_template.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading packaging template", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
