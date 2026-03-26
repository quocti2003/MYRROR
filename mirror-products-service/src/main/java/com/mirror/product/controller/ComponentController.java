package com.mirror.product.controller;

import com.mirror.product.dto.ComponentRequest;
import com.mirror.product.dto.ComponentResponse;
import com.mirror.product.entity.Component;
import com.mirror.product.service.ComponentService;
import com.mirror.product.mapper.ComponentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController {

    private final ComponentService componentService;
    private final ComponentMapper componentMapper;

    @GetMapping
    public ResponseEntity<List<ComponentResponse>> getAllComponents() {
        List<Component> components = componentService.findAllActive();
        List<ComponentResponse> responses = components.stream()
                .map(componentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{componentId}")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable String componentId) {
        return componentService.findActiveById(componentId)
                .map(componentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ComponentResponse>> getComponentsByProductId(@PathVariable String productId) {
        List<Component> components = componentService.findByProductId(productId);
        List<ComponentResponse> responses = components.stream()
                .map(componentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/name/{componentName}")
    public ResponseEntity<ComponentResponse> getComponentByName(@PathVariable String componentName) {
        return componentService.findByComponentName(componentName)
                .map(componentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createComponent(@Valid @RequestBody ComponentRequest request) {
        try {
            Component component = componentMapper.toEntity(request);
            Component savedComponent = componentService.save(component);
            ComponentResponse response = componentMapper.toResponse(savedComponent);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating component");
        }
    }

    @PutMapping("/{componentId}")
    public ResponseEntity<?> updateComponent(@PathVariable String componentId, @RequestBody ComponentRequest request) {
        try {
            Component existingComponent = componentService.findActiveById(componentId)
                    .orElseThrow(() -> new RuntimeException("Component not found"));
            componentMapper.updateEntityFromRequest(existingComponent, request);
            Component updatedComponent = componentService.save(existingComponent);
            ComponentResponse response = componentMapper.toResponse(updatedComponent);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating component");
        }
    }

    @DeleteMapping("/{componentId}")
    public ResponseEntity<?> deleteComponent(@PathVariable String componentId) {
        try {
            componentService.softDeleteById(componentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting component");
        }
    }

    @PatchMapping("/{componentId}/deactivate")
    public ResponseEntity<?> deactivateComponent(@PathVariable String componentId) {
        try {
            componentService.deactivateById(componentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deactivating component");
        }
    }

    @GetMapping("/exists/{componentName}/product/{productId}")
    public ResponseEntity<Boolean> checkComponentExists(@PathVariable String componentName, @PathVariable String productId) {
        boolean exists = componentService.existsByComponentNameAndProductId(componentName, productId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        long count = componentService.countActive();
        return ResponseEntity.ok(count);
    }
}
