package com.mirror.product.controller;

import com.mirror.product.dto.AppointmentRequest;
import com.mirror.product.dto.AppointmentResponse;
import com.mirror.product.dto.BookedSlotsResponse;
import com.mirror.product.enums.AppointmentStatus;
import com.mirror.product.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Create a new appointment
     * POST /api/appointments
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AppointmentRequest request) {
        try {
            AppointmentResponse response = appointmentService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "SLOT_UNAVAILABLE",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all appointments
     * GET /api/appointments
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAll() {
        return ResponseEntity.ok(appointmentService.getAll());
    }

    /**
     * Get appointment by ID
     * GET /api/appointments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable String id) {
        return appointmentService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get booked time slots for a date (optionally filtered by venue)
     * GET /api/appointments/slots?date=2026-01-14&venueId=LOC000001
     */
    @GetMapping("/slots")
    public ResponseEntity<BookedSlotsResponse> getBookedSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String venueId) {
        return ResponseEntity.ok(appointmentService.getBookedSlots(venueId, date));
    }

    /**
     * Get unavailable dates for a venue within a date range
     * GET /api/appointments/unavailable-dates?startDate=2026-01-01&endDate=2026-01-31&venueId=LOC000001
     * Returns dates where ALL time slots are blocked/booked
     */
    @GetMapping("/unavailable-dates")
    public ResponseEntity<Map<String, Object>> getUnavailableDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String venueId) {
        List<LocalDate> unavailableDates = appointmentService.getUnavailableDates(venueId, startDate, endDate);
        return ResponseEntity.ok(Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "venueId", venueId != null ? venueId : "",
                "unavailableDates", unavailableDates
        ));
    }

    /**
     * Get appointments by date
     * GET /api/appointments/by-date?date=2026-01-14
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<AppointmentResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getByDate(date));
    }

    /**
     * Get appointments by venue and date
     * GET /api/appointments/by-venue-date?venueId=LOC000001&date=2026-01-14
     */
    @GetMapping("/by-venue-date")
    public ResponseEntity<List<AppointmentResponse>> getByVenueAndDate(
            @RequestParam String venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getByVenueAndDate(venueId, date));
    }

    /**
     * Get upcoming appointments
     * GET /api/appointments/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<AppointmentResponse>> getUpcoming() {
        return ResponseEntity.ok(appointmentService.getUpcoming());
    }

    /**
     * Get appointments by customer email
     * GET /api/appointments/by-email?email=customer@example.com
     */
    @GetMapping("/by-email")
    public ResponseEntity<List<AppointmentResponse>> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(appointmentService.getByCustomerEmail(email));
    }

    /**
     * Get appointments by status
     * GET /api/appointments/by-status?status=PENDING
     */
    @GetMapping("/by-status")
    public ResponseEntity<List<AppointmentResponse>> getByStatus(@RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.getByStatus(status));
    }

    /**
     * Update appointment
     * PUT /api/appointments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @Valid @RequestBody AppointmentRequest request) {
        try {
            return appointmentService.update(id, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "SLOT_UNAVAILABLE",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Confirm appointment
     * POST /api/appointments/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable String id) {
        return appointmentService.confirm(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Complete appointment
     * POST /api/appointments/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable String id) {
        return appointmentService.complete(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel appointment
     * POST /api/appointments/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return appointmentService.cancel(id, reason)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mark as no-show
     * POST /api/appointments/{id}/no-show
     */
    @PostMapping("/{id}/no-show")
    public ResponseEntity<AppointmentResponse> markNoShow(@PathVariable String id) {
        return appointmentService.markNoShow(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Add staff notes
     * PUT /api/appointments/{id}/notes
     */
    @PutMapping("/{id}/notes")
    public ResponseEntity<AppointmentResponse> addNotes(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String notes = body.get("notes");
        return appointmentService.addStaffNotes(id, notes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete appointment (soft delete)
     * DELETE /api/appointments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        boolean deleted = appointmentService.delete(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "message", "Appointment deleted successfully",
                    "id", id
            ));
        }
        return ResponseEntity.notFound().build();
    }
}
