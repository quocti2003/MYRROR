package com.mirror.product.service;

import com.mirror.product.entity.AgeGroup;
import com.mirror.product.repository.AgeGroupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Age Group management - Customer demographic segments for collection planning
 * Migrated from mirror-mrp-service
 */
@Service
@RequiredArgsConstructor
public class AgeGroupService {

    private static final Logger logger = LoggerFactory.getLogger(AgeGroupService.class);

    private final AgeGroupRepository ageGroupRepository;

    /**
     * Get all age groups
     */
    @Transactional(readOnly = true)
    public List<AgeGroup> findAll() {
        return ageGroupRepository.findAll();
    }

    /**
     * Find age group by ID
     */
    @Transactional(readOnly = true)
    public Optional<AgeGroup> findById(UUID id) {
        return ageGroupRepository.findById(id);
    }

    /**
     * Find age group by name
     */
    @Transactional(readOnly = true)
    public Optional<AgeGroup> findByName(String name) {
        return ageGroupRepository.findByName(name);
    }

    /**
     * Create new age group
     */
    @Transactional
    public AgeGroup create(AgeGroup ageGroup) {
        // Validate age group name is unique
        if (ageGroup.getName() != null && ageGroupRepository.findByName(ageGroup.getName()).isPresent()) {
            throw new IllegalArgumentException("Age group with name '" + ageGroup.getName() + "' already exists");
        }

        // Validate age range
        if (ageGroup.getMinAge() != null && ageGroup.getMaxAge() != null) {
            if (ageGroup.getMinAge() > ageGroup.getMaxAge()) {
                throw new IllegalArgumentException("Min age cannot be greater than max age");
            }
        }

        logger.info("Creating new age group: {} (age range: {}-{})",
                ageGroup.getName(), ageGroup.getMinAge(), ageGroup.getMaxAge());

        return ageGroupRepository.save(ageGroup);
    }

    /**
     * Update existing age group
     */
    @Transactional
    public AgeGroup update(UUID id, AgeGroup ageGroupDetails) {
        AgeGroup ageGroup = ageGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Age group not found with id: " + id));

        // Validate name uniqueness if changed
        if (ageGroupDetails.getName() != null && !ageGroup.getName().equals(ageGroupDetails.getName())) {
            if (ageGroupRepository.findByName(ageGroupDetails.getName()).isPresent()) {
                throw new IllegalArgumentException("Age group with name '" + ageGroupDetails.getName() + "' already exists");
            }
            ageGroup.setName(ageGroupDetails.getName());
        }

        // Update age range
        if (ageGroupDetails.getMinAge() != null) {
            ageGroup.setMinAge(ageGroupDetails.getMinAge());
        }
        if (ageGroupDetails.getMaxAge() != null) {
            ageGroup.setMaxAge(ageGroupDetails.getMaxAge());
        }

        // Validate age range
        if (ageGroup.getMinAge() != null && ageGroup.getMaxAge() != null) {
            if (ageGroup.getMinAge() > ageGroup.getMaxAge()) {
                throw new IllegalArgumentException("Min age cannot be greater than max age");
            }
        }

        logger.info("Updating age group: {} (ID: {})", ageGroup.getName(), id);

        return ageGroupRepository.save(ageGroup);
    }

    /**
     * Delete age group by ID
     */
    @Transactional
    public void deleteById(UUID id) {
        if (!ageGroupRepository.existsById(id)) {
            throw new RuntimeException("Age group not found with id: " + id);
        }

        // Check if age group is used in any purchase orders or allocations
        AgeGroup ageGroup = ageGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Age group not found with id: " + id));

        int usageCount = 0;
        if (ageGroup.getPurchaseOrderAgeGroupAllocations() != null) {
            usageCount += ageGroup.getPurchaseOrderAgeGroupAllocations().size();
        }
        if (ageGroup.getOrderItemVariants() != null) {
            usageCount += ageGroup.getOrderItemVariants().size();
        }

        if (usageCount > 0) {
            throw new IllegalStateException("Cannot delete age group. It is used in " +
                    usageCount + " purchase order allocation(s) or variant(s)");
        }

        logger.warn("Deleting age group: {} (ID: {})", ageGroup.getName(), id);

        ageGroupRepository.deleteById(id);
    }

    /**
     * Count total age groups
     */
    @Transactional(readOnly = true)
    public long count() {
        return ageGroupRepository.count();
    }

    /**
     * Check if age group exists by ID
     */
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return ageGroupRepository.existsById(id);
    }
}
