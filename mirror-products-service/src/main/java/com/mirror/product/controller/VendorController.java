package com.mirror.product.controller;

import com.mirror.product.dto.VendorRequest;
import com.mirror.product.dto.VendorResponse;
import com.mirror.product.dto.ProductResponse;
import com.mirror.product.dto.OrderSummaryResponse;
import com.mirror.product.entity.Vendor;
import com.mirror.product.enums.VendorType;
import com.mirror.product.service.VendorService;
import com.mirror.product.service.ProductService;
import com.mirror.product.service.OrderService;
import com.mirror.product.mapper.VendorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {
    
    private final VendorService vendorService;
    private final VendorMapper vendorMapper;
    private final ProductService productService;
    private final OrderService orderService;
    
    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors(
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        List<Vendor> vendors;
        
        if ("current-user".equals(filter) && userId != null && !userId.trim().isEmpty()) {
            // Filter vendors by user ID for vendor dashboard
            vendors = vendorService.findByOwnerUserId(userId);
        } else {
            // Return all vendors for admin dashboard or when no filter
            vendors = vendorService.findAllActive();
        }
        
        List<VendorResponse> responses = vendors.stream()
                .map(vendorMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable String vendorId) {
        return vendorService.findActiveById(vendorId)
                .map(vendorMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<VendorResponse> getVendorByCode(@PathVariable String code) {
        return vendorService.findByCode(code)
                .map(vendorMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/country/{country}")
    public ResponseEntity<List<VendorResponse>> getVendorsByCountry(@PathVariable String country) {
        List<Vendor> vendors = vendorService.findByCountry(country);
        List<VendorResponse> responses = vendors.stream()
                .map(vendorMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/type/{vendorType}")
    public ResponseEntity<List<VendorResponse>> getVendorsByType(@PathVariable VendorType vendorType) {
        List<Vendor> vendors = vendorService.findByVendorType(vendorType);
        List<VendorResponse> responses = vendors.stream()
                .map(vendorMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<VendorResponse>> searchVendors(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Vendor> vendors = vendorService.searchVendors(search, pageable);
        List<VendorResponse> responses = vendors.getContent().stream()
                .map(vendorMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(vendors.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(vendors.getTotalPages()))
                .body(responses);
    }
    
    @PostMapping
    public ResponseEntity<?> createVendor(
            @Valid @RequestBody VendorRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Vendor vendor = vendorMapper.toEntity(request);
            
            // Set owner user ID from JWT token
            if (userId != null && !userId.trim().isEmpty()) {
                vendor.setOwnerUserId(userId);
            }
            
            Vendor savedVendor = vendorService.save(vendor);
            VendorResponse response = vendorMapper.toResponse(savedVendor);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating vendor");
        }
    }
    
    @PutMapping("/{vendorId}")
    public ResponseEntity<?> updateVendor(@PathVariable String vendorId, @RequestBody VendorRequest request) {
        try {
            Vendor existingVendor = vendorService.findActiveById(vendorId)
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));
            vendorMapper.updateEntity(existingVendor, request);
            Vendor updatedVendor = vendorService.save(existingVendor);
            VendorResponse response = vendorMapper.toResponse(updatedVendor);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating vendor");
        }
    }
    
    @DeleteMapping("/{vendorId}")
    public ResponseEntity<?> deleteVendor(@PathVariable String vendorId) {
        try {
            vendorService.softDeleteById(vendorId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting vendor");
        }
    }
    
    @PatchMapping("/{vendorId}/deactivate")
    public ResponseEntity<?> deactivateVendor(@PathVariable String vendorId) {
        try {
            vendorService.deactivateById(vendorId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deactivating vendor");
        }
    }
    
    @GetMapping("/exists/{code}")
    public ResponseEntity<Boolean> checkVendorExists(@PathVariable String code) {
        boolean exists = vendorService.existsByCode(code);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        long count = vendorService.countActive();
        return ResponseEntity.ok(count);
    }
    
    /**
     * Get products for a specific vendor
     * GET /api/vendors/{vendorId}/products
     */
    @GetMapping("/{vendorId}/products")
    public ResponseEntity<List<ProductResponse>> getVendorProducts(@PathVariable String vendorId) {
        try {
            List<ProductResponse> products = productService.getProductsByVendorId(vendorId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get orders assigned to a specific vendor
     * GET /api/vendors/{vendorId}/orders
     */
    @GetMapping("/{vendorId}/orders")
    public ResponseEntity<List<OrderSummaryResponse>> getVendorOrders(@PathVariable String vendorId) {
        try {
            List<OrderSummaryResponse> orders = orderService.getOrdersByVendor(vendorId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
