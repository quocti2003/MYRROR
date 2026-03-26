package com.mirror.product.controller.label;

import com.mirror.product.dto.label.*;
import com.mirror.product.enums.PrintJobStatus;
import com.mirror.product.service.label.PrintJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/print-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_PRINT')")
public class PrintJobController {

    private final PrintJobService printJobService;

    @PostMapping
    public ResponseEntity<PrintJobResponse> create(
            @Valid @RequestBody PrintJobCreateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        PrintJobResponse response = printJobService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PrintJobResponse>> getAll(
            @RequestParam(required = false) PrintJobStatus status,
            @RequestParam(required = false) String templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PrintJobResponse> jobs = printJobService.getAll(status, templateId, startDate, endDate, pageable);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrintJobResponse> getById(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean includeItems) {
        PrintJobResponse response = printJobService.getById(id, includeItems);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        printJobService.cancel(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<PrintJobResponse> retry(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        PrintJobResponse response = printJobService.retryFailed(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/{itemId}/printed")
    public ResponseEntity<Void> markItemPrinted(
            @PathVariable String itemId,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        printJobService.markItemPrinted(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/items/{itemId}/failed")
    public ResponseEntity<Void> markItemFailed(
            @PathVariable String itemId,
            @RequestParam String error) {
        printJobService.markItemFailed(itemId, error);
        return ResponseEntity.ok().build();
    }

    /**
     * Complete a print job (mark as COMPLETED)
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable String id) {
        printJobService.completePrinting(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Force complete a stuck print job
     */
    @PostMapping("/{id}/force-complete")
    public ResponseEntity<Void> forceComplete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        printJobService.forceComplete(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Reset a stuck job back to PENDING
     */
    @PostMapping("/{id}/reset")
    public ResponseEntity<Void> reset(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String userId = user != null ? user.getUsername() : "system";
        printJobService.resetToPending(id, userId);
        return ResponseEntity.ok().build();
    }
}
