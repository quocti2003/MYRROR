package com.mirror.product.service;

import com.mirror.product.dto.jtrc.*;
import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.entity.JTRCLaborComponent;
import com.mirror.product.entity.JTRCMetalComponent;
import com.mirror.product.entity.JTRCStoneComponent;
import com.mirror.product.enums.JTRCStatus;
import com.mirror.product.mapper.JTRCMapper;
import com.mirror.product.repository.JewelryTechnicalReportRepository;
import com.mirror.product.repository.JTRCLaborComponentRepository;
import com.mirror.product.repository.JTRCMetalComponentRepository;
import com.mirror.product.repository.JTRCStoneComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing JTRC (Jewelry Technical Report Card) operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JTRCService {

    private final JewelryTechnicalReportRepository jtrcRepository;
    private final JTRCMetalComponentRepository metalComponentRepository;
    private final JTRCStoneComponentRepository stoneComponentRepository;
    private final JTRCLaborComponentRepository laborComponentRepository;
    private final JTRCCostCalculationService costCalculationService;
    private final JTRCMapper jtrcMapper;

    /**
     * Find JTRC by ID with all components loaded
     */
    @Transactional(readOnly = true)
    public Optional<JewelryTechnicalReport> findByIdWithComponents(String id) {
        return jtrcRepository.findActiveByIdWithComponents(id);
    }

    /**
     * Find JTRC by ID
     */
    @Transactional(readOnly = true)
    public Optional<JewelryTechnicalReport> findById(String id) {
        return jtrcRepository.findActiveById(id);
    }

    /**
     * Find JTRC by report number
     */
    @Transactional(readOnly = true)
    public Optional<JewelryTechnicalReport> findByReportNumber(String reportNumber) {
        return jtrcRepository.findActiveByReportNumber(reportNumber);
    }

    /**
     * Get all active JTRCs with pagination
     */
    @Transactional(readOnly = true)
    public Page<JewelryTechnicalReport> findAll(Pageable pageable) {
        return jtrcRepository.findAll(pageable);
    }

    /**
     * Search JTRCs with filters
     */
    @Transactional(readOnly = true)
    public Page<JewelryTechnicalReport> search(JTRCSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) {
            return jtrcRepository.findAllActiveWithFilters(null, null, null, pageable);
        }

        // If text search is provided, use the search method
        if (criteria.getSearch() != null && !criteria.getSearch().isEmpty()) {
            return jtrcRepository.searchActive(criteria.getSearch(), pageable);
        }

        // Otherwise, use filters
        return jtrcRepository.findAllActiveWithFilters(
                criteria.getCollection(),
                criteria.getStatus(),
                criteria.getCategory(),
                pageable
        );
    }

    /**
     * Find JTRCs by collection
     */
    @Transactional(readOnly = true)
    public List<JewelryTechnicalReport> findByCollection(String collection) {
        return jtrcRepository.findActiveByCollection(collection);
    }

    /**
     * Find JTRCs by status
     */
    @Transactional(readOnly = true)
    public List<JewelryTechnicalReport> findByStatus(JTRCStatus status) {
        return jtrcRepository.findActiveByStatus(status);
    }

    /**
     * Find JTRCs by category
     */
    @Transactional(readOnly = true)
    public List<JewelryTechnicalReport> findByCategory(String category) {
        return jtrcRepository.findActiveByCategory(category);
    }

    /**
     * Get distinct collections
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCollections() {
        return jtrcRepository.findDistinctCollections();
    }

    /**
     * Get distinct seasons
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctSeasons() {
        return jtrcRepository.findDistinctSeasons();
    }

    /**
     * Get distinct categories
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return jtrcRepository.findDistinctCategories();
    }

    /**
     * Create a new JTRC
     */
    @Transactional
    public JewelryTechnicalReport create(JTRCCreateRequest request, String userId) {
        log.info("Creating new JTRC for collection: {}, category: {}", request.getCollection(), request.getCategory());

        // Create the main entity
        JewelryTechnicalReport jtrc = jtrcMapper.toEntity(request);
        jtrc.setStatus(JTRCStatus.DRAFT);
        jtrc.setCreatedBy(userId);
        jtrc.setUpdatedBy(userId);

        // Generate report number
        String reportNumber = generateReportNumber(request.getSeason());
        jtrc.setReportNumber(reportNumber);

        // Set entry date if not provided
        if (jtrc.getEntryDate() == null) {
            jtrc.setEntryDate(LocalDate.now());
        }

        // Save main entity first to get the ID
        jtrc = jtrcRepository.save(jtrc);

        // Add metal component
        if (request.getMetalComponent() != null) {
            JTRCMetalComponent metalComponent = jtrcMapper.toMetalComponentEntity(request.getMetalComponent());
            jtrc.setMetalComponent(metalComponent);
        }

        // Add stone components
        if (request.getStoneComponents() != null && !request.getStoneComponents().isEmpty()) {
            for (JTRCStoneComponentDTO stoneDTO : request.getStoneComponents()) {
                JTRCStoneComponent stoneComponent = jtrcMapper.toStoneComponentEntity(stoneDTO);
                jtrc.addStoneComponent(stoneComponent);
            }
        }

        // Add labor components
        if (request.getLaborComponents() != null && !request.getLaborComponents().isEmpty()) {
            for (JTRCLaborComponentDTO laborDTO : request.getLaborComponents()) {
                JTRCLaborComponent laborComponent = jtrcMapper.toLaborComponentEntity(laborDTO);
                jtrc.addLaborComponent(laborComponent);
            }
        }

        // Calculate all costs
        costCalculationService.recalculateAllCosts(jtrc);

        // Save with components
        jtrc = jtrcRepository.save(jtrc);

        log.info("Created JTRC with ID: {} and report number: {}", jtrc.getId(), jtrc.getReportNumber());
        return jtrc;
    }

    /**
     * Update an existing JTRC
     */
    @Transactional
    public JewelryTechnicalReport update(String id, JTRCUpdateRequest request, String userId) {
        log.info("Updating JTRC with ID: {}", id);

        JewelryTechnicalReport jtrc = jtrcRepository.findActiveByIdWithComponents(id)
                .orElseThrow(() -> new RuntimeException("JTRC not found with id: " + id));

        // Check if JTRC can be edited
        if (jtrc.getStatus() == JTRCStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update archived JTRC");
        }

        // Update main fields
        jtrcMapper.updateEntity(jtrc, request);
        jtrc.setUpdatedBy(userId);

        // Update metal component if provided
        if (request.getMetalComponent() != null) {
            if (jtrc.getMetalComponent() != null) {
                jtrcMapper.updateMetalComponentEntity(jtrc.getMetalComponent(), request.getMetalComponent());
            } else {
                JTRCMetalComponent metalComponent = jtrcMapper.toMetalComponentEntity(request.getMetalComponent());
                jtrc.setMetalComponent(metalComponent);
            }
        }

        // Update stone components if provided (replace all)
        if (request.getStoneComponents() != null) {
            // Clear existing
            if (jtrc.getStoneComponents() != null) {
                jtrc.getStoneComponents().clear();
            } else {
                jtrc.setStoneComponents(new HashSet<>());
            }
            // Add new
            for (JTRCStoneComponentDTO stoneDTO : request.getStoneComponents()) {
                JTRCStoneComponent stoneComponent = jtrcMapper.toStoneComponentEntity(stoneDTO);
                jtrc.addStoneComponent(stoneComponent);
            }
        }

        // Update labor components if provided (replace all)
        if (request.getLaborComponents() != null) {
            // Clear existing
            if (jtrc.getLaborComponents() != null) {
                jtrc.getLaborComponents().clear();
            } else {
                jtrc.setLaborComponents(new HashSet<>());
            }
            // Add new
            for (JTRCLaborComponentDTO laborDTO : request.getLaborComponents()) {
                JTRCLaborComponent laborComponent = jtrcMapper.toLaborComponentEntity(laborDTO);
                jtrc.addLaborComponent(laborComponent);
            }
        }

        // Recalculate costs
        costCalculationService.recalculateAllCosts(jtrc);

        jtrc = jtrcRepository.save(jtrc);
        log.info("Updated JTRC with ID: {}", jtrc.getId());

        return jtrc;
    }

    /**
     * Update JTRC status
     */
    @Transactional
    public JewelryTechnicalReport updateStatus(String id, JTRCStatus newStatus, String userId, String reason) {
        log.info("Updating JTRC status: id={}, newStatus={}, reason={}", id, newStatus, reason);

        JewelryTechnicalReport jtrc = jtrcRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("JTRC not found with id: " + id));

        JTRCStatus currentStatus = jtrc.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        jtrc.setStatus(newStatus);
        jtrc.setUpdatedBy(userId);

        jtrc = jtrcRepository.save(jtrc);
        log.info("Updated JTRC {} status from {} to {}", id, currentStatus, newStatus);

        return jtrc;
    }

    /**
     * Soft delete a JTRC
     */
    @Transactional
    public void softDelete(String id, String userId) {
        log.info("Soft deleting JTRC with ID: {}", id);

        JewelryTechnicalReport jtrc = jtrcRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("JTRC not found with id: " + id));

        // Only DRAFT JTRCs can be deleted
        if (jtrc.getStatus() != JTRCStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT JTRCs can be deleted");
        }

        jtrc.setIsDeleted(true);
        jtrc.setUpdatedBy(userId);
        jtrcRepository.save(jtrc);

        log.info("Soft deleted JTRC with ID: {}", id);
    }

    /**
     * Force recalculate costs for a JTRC
     */
    @Transactional
    public JewelryTechnicalReport recalculateCosts(String id) {
        log.info("Force recalculating costs for JTRC with ID: {}", id);

        JewelryTechnicalReport jtrc = jtrcRepository.findActiveByIdWithComponents(id)
                .orElseThrow(() -> new RuntimeException("JTRC not found with id: " + id));

        costCalculationService.recalculateAllCosts(jtrc);
        jtrc = jtrcRepository.save(jtrc);

        log.info("Recalculated costs for JTRC {}: totalCogsVnd={}, totalCogsUsd={}",
                id, jtrc.getTotalCogsVnd(), jtrc.getTotalCogsUsd());

        return jtrc;
    }

    /**
     * Generate a unique report number
     * Format: JTRC-{YEAR}-{SEASON}-{SEQUENCE}
     */
    private String generateReportNumber(String season) {
        int year = Year.now().getValue();
        String seasonCode = season != null ? season.toUpperCase() : "GEN";

        // Find the next sequence number
        int sequence = 1;
        String pattern = String.format("JTRC-%d-%s-", year, seasonCode);

        // This is a simplified approach - in production, you might want to use a database sequence
        while (jtrcRepository.existsActiveByReportNumber(pattern + String.format("%03d", sequence))) {
            sequence++;
        }

        return pattern + String.format("%03d", sequence);
    }

    /**
     * Validate status transitions
     */
    private void validateStatusTransition(JTRCStatus currentStatus, JTRCStatus newStatus) {
        if (currentStatus == newStatus) {
            return; // No change
        }

        switch (currentStatus) {
            case DRAFT:
                if (newStatus != JTRCStatus.APPROVED && newStatus != JTRCStatus.ARCHIVED) {
                    throw new IllegalStateException("DRAFT can only transition to APPROVED or ARCHIVED");
                }
                break;
            case APPROVED:
                if (newStatus != JTRCStatus.ARCHIVED && newStatus != JTRCStatus.DRAFT) {
                    throw new IllegalStateException("APPROVED can only transition to ARCHIVED or back to DRAFT");
                }
                break;
            case ARCHIVED:
                throw new IllegalStateException("ARCHIVED JTRCs cannot change status");
            default:
                throw new IllegalStateException("Unknown status: " + currentStatus);
        }
    }

    /**
     * Count active JTRCs
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return jtrcRepository.countActive();
    }

    /**
     * Check if JTRC exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jtrcRepository.existsActiveById(id);
    }
}
