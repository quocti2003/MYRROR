package com.mirror.product.controller;

import com.mirror.product.dto.CategoryDTO;
import com.mirror.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Get all categories synced from MISA
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        log.info("GET /api/categories - Fetching all categories");
        try {
            List<CategoryDTO> categories = categoryService.getAllCategories();
            log.info("Found {} active categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get category by MISA category ID
     * GET /api/categories/{categoryId}
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable String categoryId) {
        log.info("GET /api/categories/{} - Fetching category by ID", categoryId);
        try {
            CategoryDTO category = categoryService.getCategoryById(categoryId);
            return ResponseEntity.ok(category);
        } catch (RuntimeException e) {
            log.error("Category not found: {}", categoryId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching category: {}", categoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get root categories (no parent)
     * GET /api/categories/root
     */
    @GetMapping("/root")
    public ResponseEntity<List<CategoryDTO>> getRootCategories() {
        log.info("GET /api/categories/root - Fetching root categories");
        try {
            List<CategoryDTO> categories = categoryService.getRootCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching root categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get child categories by parent ID
     * GET /api/categories/children/{parentId}
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<List<CategoryDTO>> getChildCategories(@PathVariable String parentId) {
        log.info("GET /api/categories/children/{} - Fetching child categories", parentId);
        try {
            List<CategoryDTO> categories = categoryService.getChildCategories(parentId);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching child categories for parent: {}", parentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Note: Categories are managed in MISA ERP and synced to Mirror via the notification service.
     * To create/update/delete categories, use MISA ERP and trigger a sync.
     */
}
