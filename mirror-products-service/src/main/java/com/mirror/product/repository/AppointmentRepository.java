package com.mirror.product.repository;

import com.mirror.product.entity.Appointment;
import com.mirror.product.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends BaseRepository<Appointment, String> {

    /**
     * Find all appointments for a specific date
     */
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.isDeleted = false ORDER BY a.appointmentTime")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    /**
     * Find all appointments for a specific venue and date
     */
    @Query("SELECT a FROM Appointment a WHERE a.venueId = :venueId AND a.appointmentDate = :date AND a.isDeleted = false ORDER BY a.appointmentTime")
    List<Appointment> findByVenueAndDate(@Param("venueId") String venueId, @Param("date") LocalDate date);

    /**
     * Find booked time slots for a specific venue and date (excluding cancelled appointments)
     */
    @Query("SELECT a.appointmentTime FROM Appointment a WHERE a.venueId = :venueId AND a.appointmentDate = :date AND a.status NOT IN ('CANCELLED', 'NO_SHOW') AND a.isDeleted = false")
    List<LocalTime> findBookedSlotsByVenueAndDate(@Param("venueId") String venueId, @Param("date") LocalDate date);

    /**
     * Find booked time slots for a specific date across all venues (excluding cancelled appointments)
     */
    @Query("SELECT a.appointmentTime FROM Appointment a WHERE a.appointmentDate = :date AND a.status NOT IN ('CANCELLED', 'NO_SHOW') AND a.isDeleted = false")
    List<LocalTime> findBookedSlotsByDate(@Param("date") LocalDate date);

    /**
     * Check if a slot is already booked for a specific venue, date, and time
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.venueId = :venueId AND a.appointmentDate = :date AND a.appointmentTime = :time AND a.status NOT IN ('CANCELLED', 'NO_SHOW') AND a.isDeleted = false")
    boolean isSlotBooked(@Param("venueId") String venueId, @Param("date") LocalDate date, @Param("time") LocalTime time);

    /**
     * Find appointments by customer email
     */
    @Query("SELECT a FROM Appointment a WHERE a.customerEmail = :email AND a.isDeleted = false ORDER BY a.appointmentDate DESC, a.appointmentTime DESC")
    List<Appointment> findByCustomerEmail(@Param("email") String email);

    /**
     * Find appointments by status
     */
    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.isDeleted = false ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByStatus(@Param("status") AppointmentStatus status);

    /**
     * Find upcoming appointments (today and future)
     */
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate >= :today AND a.status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') AND a.isDeleted = false ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findUpcoming(@Param("today") LocalDate today);

    /**
     * Find appointments by venue
     */
    @Query("SELECT a FROM Appointment a WHERE a.venueId = :venueId AND a.isDeleted = false ORDER BY a.appointmentDate DESC, a.appointmentTime DESC")
    List<Appointment> findByVenue(@Param("venueId") String venueId);

    /**
     * Find all booked slots for a venue within a date range (for batch unavailable dates calculation)
     * Returns date and time pairs, excluding cancelled appointments
     */
    @Query("SELECT a.appointmentDate, a.appointmentTime FROM Appointment a WHERE a.venueId = :venueId AND a.appointmentDate >= :startDate AND a.appointmentDate <= :endDate AND a.status NOT IN ('CANCELLED', 'NO_SHOW') AND a.isDeleted = false")
    List<Object[]> findBookedSlotsByVenueAndDateRange(@Param("venueId") String venueId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all booked slots within a date range (all venues)
     * Returns date and time pairs, excluding cancelled appointments
     */
    @Query("SELECT a.appointmentDate, a.appointmentTime FROM Appointment a WHERE a.appointmentDate >= :startDate AND a.appointmentDate <= :endDate AND a.status NOT IN ('CANCELLED', 'NO_SHOW') AND a.isDeleted = false")
    List<Object[]> findBookedSlotsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
