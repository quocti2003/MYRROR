package com.mirror.product.controller;

import com.mirror.product.dto.PreciousMetalPriceResponse;
import com.mirror.product.dto.jtrc.*;
import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.enums.JTRCStatus;
import com.mirror.product.mapper.JTRCMapper;
import com.mirror.product.service.JTRCService;
import com.mirror.product.service.PreciousMetalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for JTRC (Jewelry Technical Report Card) operations
 */
@RestController
@RequestMapping("/api/v1/jtrc")
@RequiredArgsConstructor
@Slf4j
public class JTRCController {

    private final JTRCService jtrcService;
    private final JTRCMapper jtrcMapper;
    private final PreciousMetalService preciousMetalService;

    /**
     * Get all JTRCs with pagination and filters
     * GET /api/v1/jtrc
     */
    @GetMapping
    public ResponseEntity<?> getAllJTRCs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) JTRCStatus status) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            JTRCSearchCriteria criteria = JTRCSearchCriteria.builder()
                    .search(search)
                    .collection(collection)
                    .season(season)
                    .category(category)
                    .status(status)
                    .build();

            Page<JewelryTechnicalReport> jtrcPage = jtrcService.search(criteria, pageable);
            List<JTRCListResponse> responses = jtrcMapper.toListResponseList(jtrcPage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("content", responses);
            response.put("currentPage", jtrcPage.getNumber());
            response.put("totalItems", jtrcPage.getTotalElements());
            response.put("totalPages", jtrcPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting JTRCs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRCs");
        }
    }

    /**
     * Get JTRC by ID with all components
     * GET /api/v1/jtrc/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getJTRCById(@PathVariable String id) {
        try {
            return jtrcService.findByIdWithComponents(id)
                    .map(jtrcMapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting JTRC by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRC");
        }
    }

    /**
     * Get JTRC by report number
     * GET /api/v1/jtrc/by-report-number/{reportNumber}
     */
    @GetMapping("/by-report-number/{reportNumber}")
    public ResponseEntity<?> getJTRCByReportNumber(@PathVariable String reportNumber) {
        try {
            return jtrcService.findByReportNumber(reportNumber)
                    .map(jtrcMapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting JTRC by report number: {}", reportNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRC");
        }
    }

    /**
     * Create a new JTRC
     * POST /api/v1/jtrc
     */
    @PostMapping
    public ResponseEntity<?> createJTRC(
            @Valid @RequestBody JTRCCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            JewelryTechnicalReport jtrc = jtrcService.create(request, userId);
            JTRCResponse response = jtrcMapper.toResponse(jtrc);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for creating JTRC: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating JTRC", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating JTRC");
        }
    }

    /**
     * Update an existing JTRC
     * PUT /api/v1/jtrc/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJTRC(
            @PathVariable String id,
            @Valid @RequestBody JTRCUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            JewelryTechnicalReport jtrc = jtrcService.update(id, request, userId);
            JTRCResponse response = jtrcMapper.toResponse(jtrc);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating JTRC {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating JTRC: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating JTRC");
        }
    }

    /**
     * Save JTRC as draft
     * PUT /api/v1/jtrc/{id}/draft
     */
    @PutMapping("/{id}/draft")
    public ResponseEntity<?> saveJTRCAsDraft(
            @PathVariable String id,
            @RequestBody JTRCUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            JewelryTechnicalReport jtrc = jtrcService.update(id, request, userId);
            JTRCResponse response = jtrcMapper.toResponse(jtrc);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error saving JTRC draft {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error saving JTRC draft: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while saving JTRC draft");
        }
    }

    /**
     * Update JTRC status
     * PATCH /api/v1/jtrc/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateJTRCStatus(
            @PathVariable String id,
            @Valid @RequestBody JTRCStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            JewelryTechnicalReport jtrc = jtrcService.updateStatus(id, request.getStatus(), userId, request.getReason());
            JTRCResponse response = jtrcMapper.toResponse(jtrc);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.warn("Error updating JTRC status {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating JTRC status: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating JTRC status");
        }
    }

    /**
     * Soft delete a JTRC
     * DELETE /api/v1/jtrc/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJTRC(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            jtrcService.softDelete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete JTRC {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting JTRC: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting JTRC");
        }
    }

    /**
     * Force recalculate costs for a JTRC
     * POST /api/v1/jtrc/{id}/recalculate
     */
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<?> recalculateJTRCCosts(@PathVariable String id) {
        try {
            JewelryTechnicalReport jtrc = jtrcService.recalculateCosts(id);
            JTRCResponse response = jtrcMapper.toResponse(jtrc);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error recalculating JTRC costs: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while recalculating JTRC costs");
        }
    }

    /**
     * Get JTRCs by collection
     * GET /api/v1/jtrc/by-collection/{collection}
     */
    @GetMapping("/by-collection/{collection}")
    public ResponseEntity<?> getJTRCsByCollection(@PathVariable String collection) {
        try {
            List<JewelryTechnicalReport> jtrcs = jtrcService.findByCollection(collection);
            List<JTRCListResponse> responses = jtrcMapper.toListResponseList(jtrcs);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting JTRCs by collection: {}", collection, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRCs");
        }
    }

    /**
     * Get JTRCs by status
     * GET /api/v1/jtrc/by-status/{status}
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<?> getJTRCsByStatus(@PathVariable JTRCStatus status) {
        try {
            List<JewelryTechnicalReport> jtrcs = jtrcService.findByStatus(status);
            List<JTRCListResponse> responses = jtrcMapper.toListResponseList(jtrcs);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting JTRCs by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRCs");
        }
    }

    /**
     * Get JTRCs by category
     * GET /api/v1/jtrc/by-category/{category}
     */
    @GetMapping("/by-category/{category}")
    public ResponseEntity<?> getJTRCsByCategory(@PathVariable String category) {
        try {
            List<JewelryTechnicalReport> jtrcs = jtrcService.findByCategory(category);
            List<JTRCListResponse> responses = jtrcMapper.toListResponseList(jtrcs);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting JTRCs by category: {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRCs");
        }
    }

    /**
     * Get distinct collections for dropdown
     * GET /api/v1/jtrc/collections
     */
    @GetMapping("/collections")
    public ResponseEntity<?> getDistinctCollections() {
        try {
            List<String> collections = jtrcService.getDistinctCollections();
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            log.error("Error getting distinct collections", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching collections");
        }
    }

    /**
     * Get distinct seasons for dropdown
     * GET /api/v1/jtrc/seasons
     */
    @GetMapping("/seasons")
    public ResponseEntity<?> getDistinctSeasons() {
        try {
            List<String> seasons = jtrcService.getDistinctSeasons();
            return ResponseEntity.ok(seasons);
        } catch (Exception e) {
            log.error("Error getting distinct seasons", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching seasons");
        }
    }

    /**
     * Get distinct categories for dropdown
     * GET /api/v1/jtrc/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getDistinctCategories() {
        try {
            List<String> categories = jtrcService.getDistinctCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error getting distinct categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching categories");
        }
    }

    /**
     * Get JTRC count
     * GET /api/v1/jtrc/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getJTRCCount() {
        try {
            long count = jtrcService.countActive();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting JTRC count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching JTRC count");
        }
    }

    /**
     * Get current gold price in VND for JTRC form
     * GET /api/v1/jtrc/gold-price
     */
    @GetMapping("/gold-price")
    public ResponseEntity<?> getGoldPrice() {
        try {
            List<PreciousMetalPriceResponse> prices = preciousMetalService.getPricesInVND();
            return ResponseEntity.ok(extractGoldPrice(prices));
        } catch (Exception e) {
            log.error("Error fetching gold price", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching gold price");
        }
    }

    /**
     * Force refresh gold price for JTRC form
     * POST /api/v1/jtrc/gold-price/refresh
     */
    @PostMapping("/gold-price/refresh")
    public ResponseEntity<?> refreshGoldPrice() {
        try {
            List<PreciousMetalPriceResponse> prices = preciousMetalService.refreshPrices();
            // refreshPrices returns USD - need to get VND after refresh
            List<PreciousMetalPriceResponse> vndPrices = preciousMetalService.getPricesInVND();
            return ResponseEntity.ok(extractGoldPrice(vndPrices));
        } catch (Exception e) {
            log.error("Error refreshing gold price", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while refreshing gold price");
        }
    }

    private Map<String, Object> extractGoldPrice(List<PreciousMetalPriceResponse> prices) {
        PreciousMetalPriceResponse gold = prices.stream()
                .filter(p -> "GOLD".equalsIgnoreCase(p.getMetalType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Gold price not available"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pricePerGram", gold.getPricePerGram());
        result.put("exchangeRate", gold.getExchangeRate());
        result.put("lastUpdated", gold.getTimestamp());
        return result;
    }
}
