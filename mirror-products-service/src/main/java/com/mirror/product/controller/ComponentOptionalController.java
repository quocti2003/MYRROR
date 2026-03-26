package com.mirror.product.controller;

import com.mirror.product.dto.ComponentOptionalRequest;
import com.mirror.product.dto.ComponentOptionalResponse;
import com.mirror.product.entity.ComponentOptional;
import com.mirror.product.service.ComponentOptionalService;
import com.mirror.product.mapper.ComponentOptionalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/component-optionals")
@RequiredArgsConstructor
public class ComponentOptionalController {
    
    private final ComponentOptionalService componentOptionalService;
    private final ComponentOptionalMapper componentOptionalMapper;
    
    @GetMapping
    public ResponseEntity<List<ComponentOptionalResponse>> getAllComponentOptionals() {
        List<ComponentOptional> componentOptionals = componentOptionalService.findAllActive();
        List<ComponentOptionalResponse> responses = componentOptionals.stream()
                .map(componentOptionalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{componentOptionalId}")
    public ResponseEntity<ComponentOptionalResponse> getComponentOptionalById(@PathVariable String componentOptionalId) {
        return componentOptionalService.findActiveById(componentOptionalId)
                .map(componentOptionalMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/component/{componentId}")
    public ResponseEntity<List<ComponentOptionalResponse>> getComponentOptionalsByComponentId(@PathVariable String componentId) {
        List<ComponentOptional> componentOptionals = componentOptionalService.findByComponentId(componentId);
        List<ComponentOptionalResponse> responses = componentOptionals.stream()
                .map(componentOptionalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/name/{componentOptionalName}")
    public ResponseEntity<ComponentOptionalResponse> getComponentOptionalByName(@PathVariable String componentOptionalName) {
        return componentOptionalService.findByComponentOptionalName(componentOptionalName)
                .map(componentOptionalMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createComponentOptional(@Valid @RequestBody ComponentOptionalRequest request) {
        try {
            ComponentOptional componentOptional = componentOptionalMapper.toEntity(request);
            ComponentOptional savedComponentOptional = componentOptionalService.save(componentOptional);
            ComponentOptionalResponse response = componentOptionalMapper.toResponse(savedComponentOptional);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating component optional");
        }
    }
    
    @PutMapping("/{componentOptionalId}")
    public ResponseEntity<?> updateComponentOptional(@PathVariable String componentOptionalId, @RequestBody ComponentOptionalRequest request) {
        try {
            ComponentOptional existingComponentOptional = componentOptionalService.findActiveById(componentOptionalId)
                    .orElseThrow(() -> new RuntimeException("Component optional not found"));
            componentOptionalMapper.updateEntityFromRequest(existingComponentOptional, request);
            ComponentOptional updatedComponentOptional = componentOptionalService.save(existingComponentOptional);
            ComponentOptionalResponse response = componentOptionalMapper.toResponse(updatedComponentOptional);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating component optional");
        }
    }
    
    @DeleteMapping("/{componentOptionalId}")
    public ResponseEntity<?> deleteComponentOptional(@PathVariable String componentOptionalId) {
        try {
            componentOptionalService.softDeleteById(componentOptionalId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting component optional");
        }
    }
    
    @PatchMapping("/{componentOptionalId}/deactivate")
    public ResponseEntity<?> deactivateComponentOptional(@PathVariable String componentOptionalId) {
        try {
            componentOptionalService.deactivateById(componentOptionalId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deactivating component optional");
        }
    }
    
    @GetMapping("/exists/{componentOptionalName}/component/{componentId}")
    public ResponseEntity<Boolean> checkComponentOptionalExists(@PathVariable String componentOptionalName, @PathVariable String componentId) {
        boolean exists = componentOptionalService.existsByComponentOptionalNameAndComponentId(componentOptionalName, componentId);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        long count = componentOptionalService.countActive();
        return ResponseEntity.ok(count);
    }
}