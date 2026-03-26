package com.mirror.product.service;

import com.mirror.product.dto.dropdown.DropdownOptionResponse;
import com.mirror.product.entity.*;
import com.mirror.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing dropdown configuration options.
 * Provides read-only access to pre-configured dropdown options for SKU generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DropdownConfigurationService {

    private final SkuPrefixOptionRepository prefixRepository;
    private final MaterialOptionRepository materialRepository;
    private final MaterialColorOptionRepository materialColorRepository;
    private final StoneOriginOptionRepository stoneOriginRepository;
    private final StoneShapeOptionRepository stoneShapeRepository;
    private final StoneWeightOptionRepository stoneWeightRepository;
    private final SideStonesOptionRepository sideStonesRepository;
    private final CountryOfOriginOptionRepository countryRepository;
    private final MetalTypeOptionRepository metalTypeRepository;
    private final MetalPurityOptionRepository metalPurityRepository;
    private final StoneTypeOptionRepository stoneTypeRepository;
    private final StoneRoleOptionRepository stoneRoleRepository;
    private final ColorGradeOptionRepository colorGradeRepository;
    private final ClarityGradeOptionRepository clarityGradeRepository;
    private final LaborTypeOptionRepository laborTypeRepository;

    // ===================== SKU Prefix Options =====================

    @Transactional(readOnly = true)
    public List<SkuPrefixOption> getAllPrefixOptions() {
        return prefixRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<SkuPrefixOption> getAllPrefixOptionsIncludingInactive() {
        return prefixRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Material Options =====================

    @Transactional(readOnly = true)
    public List<MaterialOption> getAllMaterialOptions() {
        return materialRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<MaterialOption> getAllMaterialOptionsIncludingInactive() {
        return materialRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Material Color Options =====================

    @Transactional(readOnly = true)
    public List<MaterialColorOption> getAllMaterialColorOptions() {
        return materialColorRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<MaterialColorOption> getAllMaterialColorOptionsIncludingInactive() {
        return materialColorRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Stone Origin Options =====================

    @Transactional(readOnly = true)
    public List<StoneOriginOption> getAllStoneOriginOptions() {
        return stoneOriginRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<StoneOriginOption> getAllStoneOriginOptionsIncludingInactive() {
        return stoneOriginRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Stone Shape Options =====================

    @Transactional(readOnly = true)
    public List<StoneShapeOption> getAllStoneShapeOptions() {
        return stoneShapeRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<StoneShapeOption> getAllStoneShapeOptionsIncludingInactive() {
        return stoneShapeRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Stone Weight Options =====================

    @Transactional(readOnly = true)
    public List<StoneWeightOption> getAllStoneWeightOptions() {
        return stoneWeightRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<StoneWeightOption> getAllStoneWeightOptionsIncludingInactive() {
        return stoneWeightRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Side Stones Options =====================

    @Transactional(readOnly = true)
    public List<SideStonesOption> getAllSideStonesOptions() {
        return sideStonesRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<SideStonesOption> getAllSideStonesOptionsIncludingInactive() {
        return sideStonesRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Country of Origin Options =====================

    @Transactional(readOnly = true)
    public List<CountryOfOriginOption> getAllCountryOfOriginOptions() {
        return countryRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    @Transactional(readOnly = true)
    public List<CountryOfOriginOption> getAllCountryOfOriginOptionsIncludingInactive() {
        return countryRepository.findAllByOrderByDisplayOrder();
    }

    // ===================== Metal Type Options =====================

    @Transactional(readOnly = true)
    public List<MetalTypeOption> getAllMetalTypeOptions() {
        return metalTypeRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Metal Purity Options =====================

    @Transactional(readOnly = true)
    public List<MetalPurityOption> getAllMetalPurityOptions() {
        return metalPurityRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Stone Type Options =====================

    @Transactional(readOnly = true)
    public List<StoneTypeOption> getAllStoneTypeOptions() {
        return stoneTypeRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Stone Role Options =====================

    @Transactional(readOnly = true)
    public List<StoneRoleOption> getAllStoneRoleOptions() {
        return stoneRoleRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Color Grade Options =====================

    @Transactional(readOnly = true)
    public List<ColorGradeOption> getAllColorGradeOptions() {
        return colorGradeRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Clarity Grade Options =====================

    @Transactional(readOnly = true)
    public List<ClarityGradeOption> getAllClarityGradeOptions() {
        return clarityGradeRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Labor Type Options =====================

    @Transactional(readOnly = true)
    public List<LaborTypeOption> getAllLaborTypeOptions() {
        return laborTypeRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    // ===================== Validation Methods =====================

    /**
     * Validates if a material code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidMaterialCode(String materialCode) {
        if (materialCode == null || materialCode.isEmpty()) {
            return false;
        }
        return materialRepository.findByMaterialCode(materialCode)
                .map(MaterialOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a material color code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidMaterialColorCode(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return false;
        }
        return materialColorRepository.findByColorCode(colorCode)
                .map(MaterialColorOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a stone origin code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidStoneOriginCode(String originCode) {
        if (originCode == null || originCode.isEmpty()) {
            return false;
        }
        return stoneOriginRepository.findByOriginCode(originCode)
                .map(StoneOriginOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a stone shape code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidStoneShapeCode(String shapeCode) {
        if (shapeCode == null || shapeCode.isEmpty()) {
            return false;
        }
        return stoneShapeRepository.findByShapeCode(shapeCode)
                .map(StoneShapeOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a stone weight code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidStoneWeightCode(String weightCode) {
        if (weightCode == null || weightCode.isEmpty()) {
            return false;
        }
        return stoneWeightRepository.findByWeightCode(weightCode)
                .map(StoneWeightOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a side stones code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidSideStonesCode(String stoneCode) {
        if (stoneCode == null || stoneCode.isEmpty()) {
            return false;
        }
        return sideStonesRepository.findByStoneCode(stoneCode)
                .map(SideStonesOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a country code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return false;
        }
        return countryRepository.findByCountryCode(countryCode)
                .map(CountryOfOriginOption::getIsActive)
                .orElse(false);
    }

    /**
     * Validates if a prefix code exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isValidPrefixCode(String prefixCode) {
        if (prefixCode == null || prefixCode.isEmpty()) {
            return false;
        }
        return prefixRepository.findByPrefixCode(prefixCode)
                .map(SkuPrefixOption::getIsActive)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidMetalTypeCode(String typeCode) {
        if (typeCode == null || typeCode.isEmpty()) return false;
        return metalTypeRepository.findByTypeCode(typeCode).map(MetalTypeOption::getIsActive).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidMetalPurityCode(String purityCode) {
        if (purityCode == null || purityCode.isEmpty()) return false;
        return metalPurityRepository.findByPurityCode(purityCode).map(MetalPurityOption::getIsActive).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidStoneTypeCode(String typeCode) {
        if (typeCode == null || typeCode.isEmpty()) return false;
        return stoneTypeRepository.findByTypeCode(typeCode).map(StoneTypeOption::getIsActive).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidStoneRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isEmpty()) return false;
        return stoneRoleRepository.findByRoleCode(roleCode).map(StoneRoleOption::getIsActive).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidColorGradeCode(String gradeCode) {
        if (gradeCode == null || gradeCode.isEmpty()) return false;
        return colorGradeRepository.findByGradeCode(gradeCode).map(ColorGradeOption::getIsActive).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidClarityGradeCode(String clarityCode) {
        if (clarityCode == null || clarityCode.isEmpty()) return false;
        return clarityGradeRepository.findByClarityCode(clarityCode).map(ClarityGradeOption::getIsActive).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isValidLaborTypeCode(String laborCode) {
        if (laborCode == null || laborCode.isEmpty()) return false;
        return laborTypeRepository.findByLaborCode(laborCode).map(LaborTypeOption::getIsActive).orElse(false);
    }

    // ===================== DTO Mapping Methods =====================

    /**
     * Get all prefix options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getPrefixOptionsAsDto() {
        return getAllPrefixOptions().stream()
                .map(this::mapPrefixToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all material options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getMaterialOptionsAsDto() {
        return getAllMaterialOptions().stream()
                .map(this::mapMaterialToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all material color options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getMaterialColorOptionsAsDto() {
        return getAllMaterialColorOptions().stream()
                .map(this::mapMaterialColorToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all stone origin options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getStoneOriginOptionsAsDto() {
        return getAllStoneOriginOptions().stream()
                .map(this::mapStoneOriginToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all stone shape options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getStoneShapeOptionsAsDto() {
        return getAllStoneShapeOptions().stream()
                .map(this::mapStoneShapeToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all stone weight options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getStoneWeightOptionsAsDto() {
        return getAllStoneWeightOptions().stream()
                .map(this::mapStoneWeightToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all side stones options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getSideStonesOptionsAsDto() {
        return getAllSideStonesOptions().stream()
                .map(this::mapSideStonesToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all country of origin options as DTOs
     */
    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getCountryOfOriginOptionsAsDto() {
        return getAllCountryOfOriginOptions().stream()
                .map(this::mapCountryToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getMetalTypeOptionsAsDto() {
        return getAllMetalTypeOptions().stream().map(this::mapMetalTypeToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getMetalPurityOptionsAsDto() {
        return getAllMetalPurityOptions().stream().map(this::mapMetalPurityToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getStoneTypeOptionsAsDto() {
        return getAllStoneTypeOptions().stream().map(this::mapStoneTypeToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getStoneRoleOptionsAsDto() {
        return getAllStoneRoleOptions().stream().map(this::mapStoneRoleToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getColorGradeOptionsAsDto() {
        return getAllColorGradeOptions().stream().map(this::mapColorGradeToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getClarityGradeOptionsAsDto() {
        return getAllClarityGradeOptions().stream().map(this::mapClarityGradeToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DropdownOptionResponse> getLaborTypeOptionsAsDto() {
        return getAllLaborTypeOptions().stream().map(this::mapLaborTypeToDto).collect(Collectors.toList());
    }

    // ===================== Private Mapping Methods =====================

    private DropdownOptionResponse mapPrefixToDto(SkuPrefixOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getPrefixCode())
                .name(entity.getPrefixName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapMaterialToDto(MaterialOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getMaterialCode())
                .name(entity.getMaterialName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapMaterialColorToDto(MaterialColorOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getColorCode())
                .name(entity.getColorName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapStoneOriginToDto(StoneOriginOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getOriginCode())
                .name(entity.getOriginName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapStoneShapeToDto(StoneShapeOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getShapeCode())
                .name(entity.getShapeName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapStoneWeightToDto(StoneWeightOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getWeightCode())
                .name(entity.getWeightRange())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapSideStonesToDto(SideStonesOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getStoneCode())
                .name(entity.getStoneType())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapCountryToDto(CountryOfOriginOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getCountryCode())
                .name(entity.getCountryName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapMetalTypeToDto(MetalTypeOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getTypeCode())
                .name(entity.getTypeName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapMetalPurityToDto(MetalPurityOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getPurityCode())
                .name(entity.getPurityName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapStoneTypeToDto(StoneTypeOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getTypeCode())
                .name(entity.getTypeName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapStoneRoleToDto(StoneRoleOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getRoleCode())
                .name(entity.getRoleName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapColorGradeToDto(ColorGradeOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getGradeCode())
                .name(entity.getGradeName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapClarityGradeToDto(ClarityGradeOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getClarityCode())
                .name(entity.getClarityName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    private DropdownOptionResponse mapLaborTypeToDto(LaborTypeOption entity) {
        return DropdownOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getLaborCode())
                .name(entity.getLaborName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }
}
