package com.mirror.product.service;

import com.mirror.product.dto.AppointmentRequest;
import com.mirror.product.dto.AppointmentResponse;
import com.mirror.product.dto.BookedSlotsResponse;
import com.mirror.product.entity.Appointment;
import com.mirror.product.entity.Location;
import com.mirror.product.enums.AppointmentStatus;
import com.mirror.product.mapper.AppointmentMapper;
import com.mirror.product.repository.AppointmentRepository;
import com.mirror.product.repository.LocationRepository;
import com.mirror.product.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final LocationRepository locationRepository;
    private final NotificationPublisher notificationPublisher;
    private final BlockedSlotService blockedSlotService;

    // Default time slots from 9:00 AM to 6:00 PM (last slot at 6:00 PM)
    private static final List<LocalTime> DEFAULT_TIME_SLOTS = List.of(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            LocalTime.of(13, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            LocalTime.of(17, 0),
            LocalTime.of(18, 0)
    );

    /**
     * Create a new appointment
     */
    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        // Check if slot is blocked by admin
        boolean isBlocked = blockedSlotService.isSlotBlocked(
                request.getVenueId(),
                request.getAppointmentDate(),
                request.getAppointmentTime()
        );

        if (isBlocked) {
            throw new IllegalStateException("This time slot is blocked and not available for booking.");
        }

        // Check if slot is already booked
        boolean isBooked = appointmentRepository.isSlotBooked(
                request.getVenueId(),
                request.getAppointmentDate(),
                request.getAppointmentTime()
        );

        if (isBooked) {
            throw new IllegalStateException("This time slot is already booked. Please choose another time.");
        }

        Appointment appointment = appointmentMapper.toEntity(request);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Created appointment {} for {} {} at {} on {}",
                saved.getId(),
                saved.getCustomerFirstName(),
                saved.getCustomerLastName(),
                saved.getAppointmentTime(),
                saved.getAppointmentDate());

        // Send confirmation email if customer email is provided
        if (saved.getCustomerEmail() != null && !saved.getCustomerEmail().isEmpty()) {
            try {
                Location venue = locationRepository.findById(saved.getVenueId()).orElse(null);
                String venueName = venue != null ? venue.getName() : "Mirror Diamond";
                String venueAddress = venue != null ? venue.getAddress() : "";

                notificationPublisher.publishAppointmentCreated(saved, venueName, venueAddress);
            } catch (Exception e) {
                log.warn("Failed to send appointment confirmation email for {}: {}", saved.getId(), e.getMessage());
            }
        }

        return appointmentMapper.toResponse(saved);
    }

    /**
     * Get appointment by ID
     */
    @Transactional(readOnly = true)
    public Optional<AppointmentResponse> getById(String id) {
        return appointmentRepository.findActiveById(id)
                .map(appointmentMapper::toResponse);
    }

    /**
     * Get all appointments
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAll() {
        return appointmentMapper.toResponseList(appointmentRepository.findAllActive());
    }

    /**
     * Get appointments by date
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByDate(LocalDate date) {
        return appointmentMapper.toResponseList(appointmentRepository.findByDate(date));
    }

    /**
     * Get appointments by venue and date
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByVenueAndDate(String venueId, LocalDate date) {
        return appointmentMapper.toResponseList(appointmentRepository.findByVenueAndDate(venueId, date));
    }

    /**
     * Get booked slots for a specific venue and date (includes blocked slots)
     */
    @Transactional(readOnly = true)
    public BookedSlotsResponse getBookedSlots(String venueId, LocalDate date) {
        List<LocalTime> bookedSlots;

        if (venueId != null && !venueId.trim().isEmpty()) {
            bookedSlots = appointmentRepository.findBookedSlotsByVenueAndDate(venueId, date);
        } else {
            bookedSlots = appointmentRepository.findBookedSlotsByDate(date);
        }

        // Get blocked slots from admin
        List<LocalTime> blockedTimes = blockedSlotService.getBlockedTimesForVenueAndDate(
                venueId != null && !venueId.trim().isEmpty() ? venueId : null,
                date
        );

        // Combine booked and blocked slots (use Set to avoid duplicates)
        Set<LocalTime> unavailableSlots = new HashSet<>(bookedSlots);
        unavailableSlots.addAll(blockedTimes);

        // Calculate available slots
        List<LocalTime> availableSlots = new ArrayList<>(DEFAULT_TIME_SLOTS);
        availableSlots.removeAll(unavailableSlots);

        // Return combined unavailable slots as booked
        return new BookedSlotsResponse(date, venueId, new ArrayList<>(unavailableSlots), availableSlots);
    }

    /**
     * Get upcoming appointments
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUpcoming() {
        return appointmentMapper.toResponseList(appointmentRepository.findUpcoming(LocalDate.now()));
    }

    /**
     * Get appointments by customer email
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByCustomerEmail(String email) {
        return appointmentMapper.toResponseList(appointmentRepository.findByCustomerEmail(email));
    }

    /**
     * Get appointments by status
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByStatus(AppointmentStatus status) {
        return appointmentMapper.toResponseList(appointmentRepository.findByStatus(status));
    }

    /**
     * Update appointment
     */
    @Transactional
    public Optional<AppointmentResponse> update(String id, AppointmentRequest request) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    // If date/time changed, check if new slot is available
                    if ((request.getAppointmentDate() != null && !request.getAppointmentDate().equals(appointment.getAppointmentDate())) ||
                        (request.getAppointmentTime() != null && !request.getAppointmentTime().equals(appointment.getAppointmentTime()))) {

                        LocalDate newDate = request.getAppointmentDate() != null ? request.getAppointmentDate() : appointment.getAppointmentDate();
                        LocalTime newTime = request.getAppointmentTime() != null ? request.getAppointmentTime() : appointment.getAppointmentTime();
                        String venueId = request.getVenueId() != null ? request.getVenueId() : appointment.getVenueId();

                        boolean isBooked = appointmentRepository.isSlotBooked(venueId, newDate, newTime);
                        if (isBooked) {
                            throw new IllegalStateException("This time slot is already booked. Please choose another time.");
                        }
                    }

                    appointmentMapper.updateEntity(appointment, request);
                    Appointment saved = appointmentRepository.save(appointment);
                    return appointmentMapper.toResponse(saved);
                });
    }

    /**
     * Confirm appointment
     */
    @Transactional
    public Optional<AppointmentResponse> confirm(String id) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    appointment.setStatus(AppointmentStatus.CONFIRMED);
                    Appointment saved = appointmentRepository.save(appointment);
                    log.info("Confirmed appointment {}", id);
                    return appointmentMapper.toResponse(saved);
                });
    }

    /**
     * Complete appointment
     */
    @Transactional
    public Optional<AppointmentResponse> complete(String id) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    appointment.setStatus(AppointmentStatus.COMPLETED);
                    Appointment saved = appointmentRepository.save(appointment);
                    log.info("Completed appointment {}", id);
                    return appointmentMapper.toResponse(saved);
                });
    }

    /**
     * Cancel appointment
     */
    @Transactional
    public Optional<AppointmentResponse> cancel(String id, String reason) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    appointment.setStatus(AppointmentStatus.CANCELLED);
                    appointment.setCancellationReason(reason);
                    Appointment saved = appointmentRepository.save(appointment);
                    log.info("Cancelled appointment {} - Reason: {}", id, reason);
                    return appointmentMapper.toResponse(saved);
                });
    }

    /**
     * Mark as no-show
     */
    @Transactional
    public Optional<AppointmentResponse> markNoShow(String id) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    appointment.setStatus(AppointmentStatus.NO_SHOW);
                    Appointment saved = appointmentRepository.save(appointment);
                    log.info("Marked appointment {} as no-show", id);
                    return appointmentMapper.toResponse(saved);
                });
    }

    /**
     * Add staff notes
     */
    @Transactional
    public Optional<AppointmentResponse> addStaffNotes(String id, String notes) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    appointment.setStaffNotes(notes);
                    Appointment saved = appointmentRepository.save(appointment);
                    return appointmentMapper.toResponse(saved);
                });
    }

    /**
     * Delete appointment (soft delete)
     */
    @Transactional
    public boolean delete(String id) {
        return appointmentRepository.findActiveById(id)
                .map(appointment -> {
                    appointment.setIsDeleted(true);
                    appointmentRepository.save(appointment);
                    log.info("Deleted appointment {}", id);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Get unavailable dates for a venue within a date range
     * A date is unavailable if ALL time slots are blocked or booked
     *
     * OPTIMIZED: Uses batch queries instead of per-day iteration for better performance
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getUnavailableDates(String venueId, LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> unavailableDates = new HashSet<>();

        // Total number of time slots per day
        final int TOTAL_SLOTS = DEFAULT_TIME_SLOTS.size();

        // Step 1: Get all blocked times for the date range in one query
        java.util.Map<LocalDate, java.util.Set<LocalTime>> blockedTimesByDate =
            blockedSlotService.getBlockedTimesForVenueAndDateRange(venueId, startDate, endDate);

        // Step 2: Get all booked slots for the date range in one query
        java.util.Map<LocalDate, java.util.Set<LocalTime>> bookedTimesByDate = new java.util.HashMap<>();
        List<Object[]> bookedSlots;
        if (venueId != null && !venueId.trim().isEmpty()) {
            bookedSlots = appointmentRepository.findBookedSlotsByVenueAndDateRange(venueId, startDate, endDate);
        } else {
            bookedSlots = appointmentRepository.findBookedSlotsByDateRange(startDate, endDate);
        }

        for (Object[] row : bookedSlots) {
            LocalDate date = (LocalDate) row[0];
            LocalTime time = (LocalTime) row[1];
            bookedTimesByDate.computeIfAbsent(date, k -> new java.util.HashSet<>()).add(time);
        }

        // Step 3: For each date, check if all slots are unavailable
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            java.util.Set<LocalTime> unavailableSlots = new java.util.HashSet<>();

            // Add blocked times
            java.util.Set<LocalTime> blocked = blockedTimesByDate.get(date);
            if (blocked != null) {
                unavailableSlots.addAll(blocked);
            }

            // Add booked times
            java.util.Set<LocalTime> booked = bookedTimesByDate.get(date);
            if (booked != null) {
                unavailableSlots.addAll(booked);
            }

            // If all slots are unavailable, mark date as unavailable
            if (unavailableSlots.size() >= TOTAL_SLOTS) {
                unavailableDates.add(date);
            }
        }

        // Sort and return
        List<LocalDate> sortedDates = new ArrayList<>(unavailableDates);
        sortedDates.sort(LocalDate::compareTo);
        return sortedDates;
    }
}
