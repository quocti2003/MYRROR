package com.mirror.product.controller;

import com.mirror.product.dto.dropdown.DropdownOptionResponse;
import com.mirror.product.service.DropdownConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for dropdown configuration options
 * Provides endpoints to fetch dropdown options for SKU generation UI
 */
@RestController
@RequestMapping("/api/dropdown-config")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DropdownConfigurationController {

    private final DropdownConfigurationService dropdownService;

    /**
     * Get all SKU prefix options
     * GET /api/dropdown-config/prefixes
     */
    @GetMapping("/prefixes")
    public ResponseEntity<List<DropdownOptionResponse>> getPrefixOptions() {
        log.debug("Fetching SKU prefix options");
        List<DropdownOptionResponse> options = dropdownService.getPrefixOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all material options
     * GET /api/dropdown-config/materials
     */
    @GetMapping("/materials")
    public ResponseEntity<List<DropdownOptionResponse>> getMaterialOptions() {
        log.debug("Fetching material options");
        List<DropdownOptionResponse> options = dropdownService.getMaterialOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all material color options
     * GET /api/dropdown-config/material-colors
     */
    @GetMapping("/material-colors")
    public ResponseEntity<List<DropdownOptionResponse>> getMaterialColorOptions() {
        log.debug("Fetching material color options");
        List<DropdownOptionResponse> options = dropdownService.getMaterialColorOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all stone origin options
     * GET /api/dropdown-config/stone-origins
     */
    @GetMapping("/stone-origins")
    public ResponseEntity<List<DropdownOptionResponse>> getStoneOriginOptions() {
        log.debug("Fetching stone origin options");
        List<DropdownOptionResponse> options = dropdownService.getStoneOriginOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all stone shape options
     * GET /api/dropdown-config/stone-shapes
     */
    @GetMapping("/stone-shapes")
    public ResponseEntity<List<DropdownOptionResponse>> getStoneShapeOptions() {
        log.debug("Fetching stone shape options");
        List<DropdownOptionResponse> options = dropdownService.getStoneShapeOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all stone weight options
     * GET /api/dropdown-config/stone-weights
     */
    @GetMapping("/stone-weights")
    public ResponseEntity<List<DropdownOptionResponse>> getStoneWeightOptions() {
        log.debug("Fetching stone weight options");
        List<DropdownOptionResponse> options = dropdownService.getStoneWeightOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all side stones options
     * GET /api/dropdown-config/side-stones
     */
    @GetMapping("/side-stones")
    public ResponseEntity<List<DropdownOptionResponse>> getSideStonesOptions() {
        log.debug("Fetching side stones options");
        List<DropdownOptionResponse> options = dropdownService.getSideStonesOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all country of origin options
     * GET /api/dropdown-config/countries
     */
    @GetMapping("/countries")
    public ResponseEntity<List<DropdownOptionResponse>> getCountryOfOriginOptions() {
        log.debug("Fetching country of origin options");
        List<DropdownOptionResponse> options = dropdownService.getCountryOfOriginOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all metal type options
     * GET /api/dropdown-config/metal-types
     */
    @GetMapping("/metal-types")
    public ResponseEntity<List<DropdownOptionResponse>> getMetalTypeOptions() {
        log.debug("Fetching metal type options");
        List<DropdownOptionResponse> options = dropdownService.getMetalTypeOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all metal purity options
     * GET /api/dropdown-config/metal-purities
     */
    @GetMapping("/metal-purities")
    public ResponseEntity<List<DropdownOptionResponse>> getMetalPurityOptions() {
        log.debug("Fetching metal purity options");
        List<DropdownOptionResponse> options = dropdownService.getMetalPurityOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all stone type options
     * GET /api/dropdown-config/stone-types
     */
    @GetMapping("/stone-types")
    public ResponseEntity<List<DropdownOptionResponse>> getStoneTypeOptions() {
        log.debug("Fetching stone type options");
        List<DropdownOptionResponse> options = dropdownService.getStoneTypeOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all stone role options
     * GET /api/dropdown-config/stone-roles
     */
    @GetMapping("/stone-roles")
    public ResponseEntity<List<DropdownOptionResponse>> getStoneRoleOptions() {
        log.debug("Fetching stone role options");
        List<DropdownOptionResponse> options = dropdownService.getStoneRoleOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all color grade options
     * GET /api/dropdown-config/color-grades
     */
    @GetMapping("/color-grades")
    public ResponseEntity<List<DropdownOptionResponse>> getColorGradeOptions() {
        log.debug("Fetching color grade options");
        List<DropdownOptionResponse> options = dropdownService.getColorGradeOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all clarity grade options
     * GET /api/dropdown-config/clarity-grades
     */
    @GetMapping("/clarity-grades")
    public ResponseEntity<List<DropdownOptionResponse>> getClarityGradeOptions() {
        log.debug("Fetching clarity grade options");
        List<DropdownOptionResponse> options = dropdownService.getClarityGradeOptionsAsDto();
        return ResponseEntity.ok(options);
    }

    /**
     * Get all labor type options
     * GET /api/dropdown-config/labor-types
     */
    @GetMapping("/labor-types")
    public ResponseEntity<List<DropdownOptionResponse>> getLaborTypeOptions() {
        log.debug("Fetching labor type options");
        List<DropdownOptionResponse> options = dropdownService.getLaborTypeOptionsAsDto();
        return ResponseEntity.ok(options);
    }
}
