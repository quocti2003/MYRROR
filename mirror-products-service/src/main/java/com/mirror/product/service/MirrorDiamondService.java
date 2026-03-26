package com.mirror.product.service;

import com.mirror.product.dto.diamond.*;
import com.mirror.product.entity.MirrorDiamond;
import com.mirror.product.repository.MirrorDiamondRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Mirror Diamond operations with Mirror SKU generation
 * SKU Format: LGD-{COLOR}{CLARITY}-{CARAT}-{SHAPE}-{ORIGIN}-{MFR}-{CERT}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MirrorDiamondService {

    private final MirrorDiamondRepository diamondRepository;

    // ==================== SKU GENERATION ====================

    /**
     * Generate Mirror SKU code for lab-grown diamond
     * Format: LGD-{COLOR}{CLARITY}-{CARAT}-{SHAPE}-{ORIGIN}-{MFR}-{CERT}
     * Example: LGD-FVS1-213-RD-IN-KARP-493218
     */
    public DiamondSkuResponse generateSku(DiamondSkuRequest request) {
        log.info("Generating SKU for diamond: {} {} {}ct", request.getColor(), request.getClarity(), request.getCaratWeight());

        // Normalize inputs
        String color = request.getColor().toUpperCase().trim();
        String clarity = request.getClarity().toUpperCase().trim();
        String shape = request.getShape().toUpperCase().trim();
        String origin = request.getOrigin().toUpperCase().trim();
        String mfr = request.getManufacturerCode().toUpperCase().trim();

        // Format carat weight (remove decimal point)
        String caratStr = formatCaratWeight(request.getCaratWeight());

        // Get last 6 digits of certificate
        String certShort = getShortCert(request.getCertNumber());

        // Build SKU: LGD-{COLOR}{CLARITY}-{CARAT}-{SHAPE}-{ORIGIN}-{MFR}-{CERT}
        String sku = String.format("LGD-%s%s-%s-%s-%s-%s-%s",
                color, clarity, caratStr, shape, origin, mfr, certShort);

        // Check uniqueness
        boolean isUnique = !diamondRepository.existsBySkuCode(sku);

        log.info("Generated SKU: {} (unique: {})", sku, isUnique);

        return DiamondSkuResponse.builder()
                .skuCode(sku)
                .message(isUnique ? "SKU generated successfully" : "Warning: SKU already exists")
                .isUnique(isUnique)
                .diamondType("LGD")
                .colorClarity(color + clarity)
                .carat(caratStr)
                .shape(shape)
                .origin(origin)
                .manufacturer(mfr)
                .certShort(certShort)
                .build();
    }

    /**
     * Format carat weight by removing decimal point
     * 2.13 → 213
     * 3.32 → 332
     * 5.07 → 507
     */
    private String formatCaratWeight(BigDecimal carat) {
        return carat.multiply(BigDecimal.valueOf(100))
                .setScale(0)
                .toString();
    }

    /**
     * Get last 6 digits of certificate number
     * 636493218 → 493218
     */
    private String getShortCert(String certNumber) {
        if (certNumber.length() <= 6) {
            return certNumber;
        }
        return certNumber.substring(certNumber.length() - 6);
    }

    // ==================== CRUD OPERATIONS ====================

    @Transactional
    public DiamondResponse createDiamond(DiamondRequest request) {
        log.info("Creating diamond: {} {} {}ct", request.getColor(), request.getClarity(), request.getCaratWeight());

        // Generate SKU automatically
        DiamondSkuRequest skuRequest = DiamondSkuRequest.builder()
                .color(request.getColor())
                .clarity(request.getClarity())
                .caratWeight(request.getCaratWeight())
                .shape(request.getShape())
                .origin(request.getOriginCountry() != null ? request.getOriginCountry() : "IN")
                .manufacturerCode(request.getManufacturerCode() != null ? request.getManufacturerCode() : "UNKN")
                .certNumber(request.getCertNumber())
                .build();

        DiamondSkuResponse skuResponse = generateSku(skuRequest);

        // Check if SKU already exists
        if (!skuResponse.isUnique()) {
            throw new RuntimeException("Diamond with SKU " + skuResponse.getSkuCode() + " already exists");
        }

        // Check if cert number already exists
        if (diamondRepository.existsByCertNumber(request.getCertNumber())) {
            throw new RuntimeException("Diamond with certificate number " + request.getCertNumber() + " already exists");
        }

        // Build entity
        MirrorDiamond diamond = MirrorDiamond.builder()
                .skuCode(skuResponse.getSkuCode())
                .color(request.getColor().toUpperCase())
                .clarity(request.getClarity().toUpperCase())
                .caratWeight(request.getCaratWeight())
                .shape(request.getShape().toUpperCase())
                .originCountry(request.getOriginCountry())
                .manufacturerCode(request.getManufacturerCode())
                .manufacturerName(request.getManufacturerName())
                .manufacturerSerial(request.getManufacturerSerial())
                .certNumber(request.getCertNumber())
                .certLab(request.getCertLab())
                .certUrl(request.getCertUrl())
                .unitPriceUsd(request.getUnitPriceUsd())
                .totalPriceUsd(request.getTotalPriceUsd())
                .invoiceNumber(request.getInvoiceNumber())
                .invoiceDate(request.getInvoiceDate())
                .customsDeclaration(request.getCustomsDeclaration())
                .importDate(request.getImportDate())
                .location(request.getLocation())
                .notes(request.getNotes())
                .status("IN_STOCK")
                .isActive(true)
                .build();

        MirrorDiamond saved = diamondRepository.save(diamond);
        log.info("Diamond created successfully: {} (ID: {})", saved.getSkuCode(), saved.getId());

        return mapToResponse(saved);
    }

    public Optional<DiamondResponse> getDiamondById(String id) {
        return diamondRepository.findById(id)
                .map(this::mapToResponse);
    }

    public Optional<DiamondResponse> getDiamondBySkuCode(String skuCode) {
        return diamondRepository.findBySkuCode(skuCode)
                .map(this::mapToResponse);
    }

    public Optional<DiamondResponse> getDiamondByCertNumber(String certNumber) {
        return diamondRepository.findByCertNumber(certNumber)
                .map(this::mapToResponse);
    }

    public List<DiamondResponse> getAllActiveDiamonds() {
        return diamondRepository.findAllActiveOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<DiamondResponse> updateDiamond(String id, DiamondRequest request) {
        log.info("Updating diamond: {}", id);

        return diamondRepository.findById(id)
                .map(diamond -> {
                    // Update fields (SKU and cert number cannot be changed)
                    diamond.setCaratWeight(request.getCaratWeight());
                    diamond.setManufacturerName(request.getManufacturerName());
                    diamond.setCertLab(request.getCertLab());
                    diamond.setCertUrl(request.getCertUrl());
                    diamond.setUnitPriceUsd(request.getUnitPriceUsd());
                    diamond.setTotalPriceUsd(request.getTotalPriceUsd());
                    diamond.setLocation(request.getLocation());
                    diamond.setNotes(request.getNotes());

                    MirrorDiamond updated = diamondRepository.save(diamond);
                    log.info("Diamond updated: {}", updated.getSkuCode());

                    return mapToResponse(updated);
                });
    }

    @Transactional
    public boolean updateDiamondStatus(String id, String status) {
        log.info("Updating diamond status: {} to {}", id, status);

        return diamondRepository.findById(id)
                .map(diamond -> {
                    diamond.setStatus(status);
                    diamondRepository.save(diamond);
                    log.info("Diamond status updated: {} → {}", diamond.getSkuCode(), status);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean deleteDiamond(String id) {
        log.info("Soft deleting diamond: {}", id);

        return diamondRepository.findById(id)
                .map(diamond -> {
                    diamond.setIsActive(false);
                    diamondRepository.save(diamond);
                    log.info("Diamond soft deleted: {}", diamond.getSkuCode());
                    return true;
                })
                .orElse(false);
    }

    // ==================== SEARCH & FILTER ====================

    public List<DiamondResponse> searchDiamonds(DiamondSearchRequest request) {
        log.info("Searching diamonds with filters: {}", request);

        return diamondRepository.searchDiamonds(
                request.getColor(),
                request.getClarity(),
                request.getShape(),
                request.getMinCarat(),
                request.getMaxCarat(),
                request.getStatus(),
                request.getManufacturerCode()
        ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DiamondResponse> getDiamondsByInvoice(String invoiceNumber) {
        return diamondRepository.findByInvoiceNumberOrderBySkuCode(invoiceNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DiamondResponse> getDiamondsByStatus(String status) {
        return diamondRepository.findByStatusAndIsActiveTrue(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== STATISTICS ====================

    public long countByStatus(String status) {
        return diamondRepository.countByStatus(status);
    }

    public BigDecimal getTotalCaratWeightInStock() {
        BigDecimal total = diamondRepository.getTotalCaratWeightInStock();
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalInventoryValueUsd() {
        BigDecimal total = diamondRepository.getTotalInventoryValueUsd();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ==================== MAPPER ====================

    private DiamondResponse mapToResponse(MirrorDiamond diamond) {
        return DiamondResponse.builder()
                .id(diamond.getId())
                .skuCode(diamond.getSkuCode())
                .color(diamond.getColor())
                .clarity(diamond.getClarity())
                .caratWeight(diamond.getCaratWeight())
                .shape(diamond.getShape())
                .originCountry(diamond.getOriginCountry())
                .manufacturerCode(diamond.getManufacturerCode())
                .manufacturerName(diamond.getManufacturerName())
                .manufacturerSerial(diamond.getManufacturerSerial())
                .certNumber(diamond.getCertNumber())
                .certLab(diamond.getCertLab())
                .certUrl(diamond.getCertUrl())
                .unitPriceUsd(diamond.getUnitPriceUsd())
                .totalPriceUsd(diamond.getTotalPriceUsd())
                .invoiceNumber(diamond.getInvoiceNumber())
                .invoiceDate(diamond.getInvoiceDate())
                .customsDeclaration(diamond.getCustomsDeclaration())
                .importDate(diamond.getImportDate())
                .status(diamond.getStatus())
                .location(diamond.getLocation())
                .mirrorProductId(diamond.getMirrorProductId())
                .notes(diamond.getNotes())
                .createdAt(diamond.getCreatedAt())
                .updatedAt(diamond.getUpdatedAt())
                .isActive(diamond.getIsActive())
                .build();
    }
}
