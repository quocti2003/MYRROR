package com.mirror.product.controller;

import com.mirror.product.dto.AgeGroupRequest;
import com.mirror.product.dto.AgeGroupResponse;
import com.mirror.product.entity.AgeGroup;
import com.mirror.product.service.AgeGroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Age Group management - Customer demographic segments
 * Migrated from mirror-mrp-service
 */
@RestController
@RequestMapping("/api/age-groups")
@CrossOrigin(origins = "*")
public class AgeGroupController {

    @Autowired
    private AgeGroupService ageGroupService;

    /**
     * Get all age groups
     * GET /api/age-groups
     */
    @GetMapping
    public ResponseEntity<List<AgeGroupResponse>> getAllAgeGroups(
            @RequestParam(value = "includePreferences", defaultValue = "false") boolean includePreferences) {
        List<AgeGroup> ageGroups = ageGroupService.findAll();
        List<AgeGroupResponse> response = ageGroups.stream()
                .map(ageGroup -> new AgeGroupResponse(ageGroup, includePreferences))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get age group by ID
     * GET /api/age-groups/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AgeGroupResponse> getAgeGroupById(
            @PathVariable UUID id,
            @RequestParam(value = "includePreferences", defaultValue = "true") boolean includePreferences) {
        return ageGroupService.findById(id)
                .map(ageGroup -> ResponseEntity.ok(new AgeGroupResponse(ageGroup, includePreferences)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get age group by name
     * GET /api/age-groups/name/{name}
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<AgeGroupResponse> getAgeGroupByName(@PathVariable String name) {
        return ageGroupService.findByName(name)
                .map(ageGroup -> ResponseEntity.ok(new AgeGroupResponse(ageGroup, true)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new age group
     * POST /api/age-groups
     */
    @PostMapping
    public ResponseEntity<?> createAgeGroup(@Valid @RequestBody AgeGroupRequest request) {
        try {
            AgeGroup ageGroup = AgeGroup.builder()
                    .name(request.getName())
                    .minAge(request.getMinAge())
                    .maxAge(request.getMaxAge())
                    .build();

            AgeGroup created = ageGroupService.create(ageGroup);
            AgeGroupResponse response = new AgeGroupResponse(created, false);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update existing age group
     * PUT /api/age-groups/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgeGroup(
            @PathVariable UUID id,
            @Valid @RequestBody AgeGroupRequest request) {
        try {
            AgeGroup ageGroupDetails = AgeGroup.builder()
                    .name(request.getName())
                    .minAge(request.getMinAge())
                    .maxAge(request.getMaxAge())
                    .build();

            AgeGroup updated = ageGroupService.update(id, ageGroupDetails);
            AgeGroupResponse response = new AgeGroupResponse(updated, false);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete age group
     * DELETE /api/age-groups/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgeGroup(@PathVariable UUID id) {
        try {
            ageGroupService.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Age group deleted successfully",
                    "id", id.toString()
            ));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get age group count
     * GET /api/age-groups/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getAgeGroupCount() {
        long count = ageGroupService.count();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
