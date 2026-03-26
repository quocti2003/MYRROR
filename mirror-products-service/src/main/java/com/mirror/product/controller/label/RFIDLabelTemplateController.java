package com.mirror.product.controller.label;

import com.mirror.product.dto.label.LabelTemplateCreateRequest;
import com.mirror.product.dto.label.LabelTemplateResponse;
import com.mirror.product.dto.label.LabelTemplateUpdateRequest;
import com.mirror.product.enums.LabelTemplateStatus;
import com.mirror.product.service.label.RFIDLabelTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/label-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_MANAGE')")
public class RFIDLabelTemplateController {

    private final RFIDLabelTemplateService templateService;

    @PostMapping
    public ResponseEntity<LabelTemplateResponse> create(
            @Valid @RequestBody LabelTemplateCreateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        LabelTemplateResponse response = templateService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<LabelTemplateResponse>> getAll(
            @RequestParam(required = false) LabelTemplateStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LabelTemplateResponse> templates = templateService.getAll(status, search, pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabelTemplateResponse> getById(@PathVariable String id) {
        LabelTemplateResponse response = templateService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabelTemplateResponse> update(
            @PathVariable String id,
            @Valid @RequestBody LabelTemplateUpdateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        LabelTemplateResponse response = templateService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        templateService.delete(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<LabelTemplateResponse> duplicate(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        LabelTemplateResponse response = templateService.duplicate(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/default")
    public ResponseEntity<LabelTemplateResponse> getDefault() {
        return templateService.getDefault()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
