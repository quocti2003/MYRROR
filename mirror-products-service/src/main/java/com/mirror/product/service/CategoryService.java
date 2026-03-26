package com.mirror.product.service;

import com.mirror.product.dto.CategoryDTO;
import com.mirror.product.entity.misa.MisaProductCategory;
import com.mirror.product.repository.misa.MisaProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final MisaProductCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        log.info("Fetching all active categories from MISA");
        return categoryRepository.findByIsInactiveFalse().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(String categoryId) {
        log.info("Fetching category with categoryId: {}", categoryId);
        MisaProductCategory category = categoryRepository.findByCategoryId(categoryId)
                .filter(MisaProductCategory::isActive)
                .orElseThrow(() -> new RuntimeException("Category not found with categoryId: " + categoryId));
        return toDTO(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getRootCategories() {
        log.info("Fetching root categories");
        return categoryRepository.findRootCategoriesOrdered().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildCategories(String parentId) {
        log.info("Fetching child categories for parent: {}", parentId);
        return categoryRepository.findChildCategoriesOrdered(parentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CategoryDTO toDTO(MisaProductCategory category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .parentId(category.getParentId())
                .grade(category.getGrade())
                .isLeaf(category.getIsLeaf())
                .isActive(!Boolean.TRUE.equals(category.getIsInactive()))
                .isInactive(category.getIsInactive())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .fullPath(category.getFullPath())
                .levelNames(category.getLevelNames())
                .misaLastModified(category.getMisaLastModified())
                .lastSyncDate(category.getLastSyncDate())
                .syncStatus(category.getSyncStatus() != null ? category.getSyncStatus().name() : null)
                .syncErrorMessage(category.getSyncErrorMessage())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
