package com.mirror.product.controller.label;

import com.mirror.product.dto.label.LabelGenerateRequest;
import com.mirror.product.dto.label.LabelGenerateResponse;
import com.mirror.product.service.label.PrintJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/labels")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('LABEL_GENERATE')")
public class LabelGenerationController {

    private final PrintJobService printJobService;

    @PostMapping("/generate-zpl")
    public ResponseEntity<LabelGenerateResponse> generateZPL(
            @Valid @RequestBody LabelGenerateRequest request) {
        LabelGenerateResponse response = printJobService.generateZPL(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-tspl")
    public ResponseEntity<LabelGenerateResponse> generateTSPL(
            @Valid @RequestBody LabelGenerateRequest request) {
        // For now, use same logic as ZPL (can be extended for TSPL-specific generation)
        LabelGenerateResponse response = printJobService.generateZPL(request);
        return ResponseEntity.ok(response);
    }
}
