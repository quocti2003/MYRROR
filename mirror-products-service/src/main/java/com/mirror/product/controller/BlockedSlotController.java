package com.mirror.product.controller;

import com.mirror.product.dto.BlockedSlotRequest;
import com.mirror.product.dto.BlockedSlotResponse;
import com.mirror.product.service.BlockedSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments/blocked-slots")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BlockedSlotController {

    private final BlockedSlotService blockedSlotService;

    /**
     * Create a new blocked slot
     */
    @PostMapping
    public ResponseEntity<BlockedSlotResponse> create(@Valid @RequestBody BlockedSlotRequest request) {
        log.info("Creating blocked slot for venue: {} on date: {}", request.getVenueId(), request.getBlockDate());
        BlockedSlotResponse response = blockedSlotService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all blocked slots
     */
    @GetMapping
    public ResponseEntity<List<BlockedSlotResponse>> getAll() {
        List<BlockedSlotResponse> response = blockedSlotService.getAll();
        return ResponseEntity.ok(response);
    }

    /**
     * Get blocked slot by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlockedSlotResponse> getById(@PathVariable String id) {
        BlockedSlotResponse response = blockedSlotService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get blocked slots for a specific date
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<BlockedSlotResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BlockedSlotResponse> response = blockedSlotService.getByDate(date);
        return ResponseEntity.ok(response);
    }

    /**
     * Get blocked slots for a specific venue and date
     */
    @GetMapping("/by-venue-date")
    public ResponseEntity<List<BlockedSlotResponse>> getByVenueAndDate(
            @RequestParam String venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BlockedSlotResponse> response = blockedSlotService.getByVenueAndDate(venueId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * Get blocked slots within a date range
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<List<BlockedSlotResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String venueId) {
        List<BlockedSlotResponse> response = blockedSlotService.getByDateRange(startDate, endDate, venueId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a specific time slot is blocked
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkSlotBlocked(
            @RequestParam String venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        boolean isBlocked = blockedSlotService.isSlotBlocked(venueId, date, time);
        return ResponseEntity.ok(Map.of("isBlocked", isBlocked));
    }

    /**
     * Get blocked times for a specific venue and date
     */
    @GetMapping("/blocked-times")
    public ResponseEntity<List<LocalTime>> getBlockedTimes(
            @RequestParam String venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<LocalTime> blockedTimes = blockedSlotService.getBlockedTimesForVenueAndDate(venueId, date);
        return ResponseEntity.ok(blockedTimes);
    }

    /**
     * Update a blocked slot
     */
    @PutMapping("/{id}")
    public ResponseEntity<BlockedSlotResponse> update(
            @PathVariable String id,
            @Valid @RequestBody BlockedSlotRequest request) {
        log.info("Updating blocked slot: {}", id);
        BlockedSlotResponse response = blockedSlotService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a blocked slot
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Deleting blocked slot: {}", id);
        blockedSlotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
