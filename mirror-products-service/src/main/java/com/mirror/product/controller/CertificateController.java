package com.mirror.product.controller;

import com.mirror.product.dto.CertificateRequest;
import com.mirror.product.dto.CertificateResponse;
import com.mirror.product.enums.CertificateType;
import com.mirror.product.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * Create a new certificate
     * POST /api/certificates
     * Required role: PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CertificateRequest request) {
        try {
            CertificateResponse response = certificateService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "DUPLICATE_CODE",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all certificates
     * GET /api/certificates
     * Public access
     */
    @GetMapping
    public ResponseEntity<List<CertificateResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAll());
    }

    /**
     * Get certificate by ID
     * GET /api/certificates/{id}
     * Public access
     */
    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getById(@PathVariable String id) {
        return certificateService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get certificate by code
     * GET /api/certificates/by-code?code=123456789
     * Public access
     */
    @GetMapping("/by-code")
    public ResponseEntity<CertificateResponse> getByCode(@RequestParam String code) {
        return certificateService.getByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get certificates by type
     * GET /api/certificates/by-type?type=IGI
     * Public access
     */
    @GetMapping("/by-type")
    public ResponseEntity<List<CertificateResponse>> getByType(@RequestParam CertificateType type) {
        return ResponseEntity.ok(certificateService.getByType(type));
    }

    /**
     * Update certificate
     * PUT /api/certificates/{id}
     * Required role: PRODUCTION_OPS, ADMIN, IT_ADMIN, SUPER_ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @Valid @RequestBody CertificateRequest request) {
        try {
            return certificateService.update(id, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "DUPLICATE_CODE",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete certificate (soft delete)
     * DELETE /api/certificates/{id}
     * Required role: ADMIN, IT_ADMIN, SUPER_ADMIN
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        boolean deleted = certificateService.delete(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "message", "Certificate deleted successfully",
                    "id", id
            ));
        }
        return ResponseEntity.notFound().build();
    }
}
