package com.mirror.product.controller;

import com.mirror.product.dto.ItemVariantRequest;
import com.mirror.product.dto.ItemVariantResponse;
import com.mirror.product.entity.ItemVariant;
import com.mirror.product.service.ItemVariantService;
import com.mirror.product.mapper.ItemVariantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/item-variants")
@RequiredArgsConstructor
public class ItemVariantController {
    
    private final ItemVariantService itemVariantService;
    private final ItemVariantMapper itemVariantMapper;
    
    @GetMapping
    public ResponseEntity<List<ItemVariantResponse>> getAllItemVariants() {
        List<ItemVariant> itemVariants = itemVariantService.findAllActive();
        List<ItemVariantResponse> responses = itemVariants.stream()
                .map(itemVariantMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{itemVariantId}")
    public ResponseEntity<ItemVariantResponse> getItemVariantById(@PathVariable String itemVariantId) {
        return itemVariantService.findActiveById(itemVariantId)
                .map(itemVariantMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/url/{itemVariantUrl}")
    public ResponseEntity<ItemVariantResponse> getItemVariantByUrl(@PathVariable String itemVariantUrl) {
        return itemVariantService.findByItemVariantUrl(itemVariantUrl)
                .map(itemVariantMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createItemVariant(@Valid @RequestBody ItemVariantRequest request) {
        try {
            ItemVariant itemVariant = itemVariantMapper.toEntity(request);
            ItemVariant savedItemVariant = itemVariantService.save(itemVariant);
            ItemVariantResponse response = itemVariantMapper.toResponse(savedItemVariant);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating item variant");
        }
    }
    
    @PutMapping("/{itemVariantId}")
    public ResponseEntity<?> updateItemVariant(@PathVariable String itemVariantId, @RequestBody ItemVariantRequest request) {
        try {
            ItemVariant existingItemVariant = itemVariantService.findActiveById(itemVariantId)
                    .orElseThrow(() -> new RuntimeException("Item variant not found"));
            itemVariantMapper.updateEntityFromRequest(existingItemVariant, request);
            ItemVariant updatedItemVariant = itemVariantService.save(existingItemVariant);
            ItemVariantResponse response = itemVariantMapper.toResponse(updatedItemVariant);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating item variant");
        }
    }
    
    @DeleteMapping("/{itemVariantId}")
    public ResponseEntity<?> deleteItemVariant(@PathVariable String itemVariantId) {
        try {
            itemVariantService.softDeleteById(itemVariantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting item variant");
        }
    }
    
    @PatchMapping("/{itemVariantId}/deactivate")
    public ResponseEntity<?> deactivateItemVariant(@PathVariable String itemVariantId) {
        try {
            itemVariantService.deactivateById(itemVariantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deactivating item variant");
        }
    }
    
    @GetMapping("/exists/{itemVariantUrl}")
    public ResponseEntity<Boolean> checkItemVariantExists(@PathVariable String itemVariantUrl) {
        boolean exists = itemVariantService.existsByItemVariantUrl(itemVariantUrl);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        long count = itemVariantService.countActive();
        return ResponseEntity.ok(count);
    }
}