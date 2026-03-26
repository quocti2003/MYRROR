package com.mirror.product.repository;

import com.mirror.product.entity.BlockedSlot;
import com.mirror.product.enums.BlockType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BlockedSlotRepository extends BaseRepository<BlockedSlot, String> {

    /**
     * Find all blocked slots for a specific date
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.blockDate = :date AND b.isDeleted = false ORDER BY b.startTime")
    List<BlockedSlot> findByDate(@Param("date") LocalDate date);

    /**
     * Find all blocked slots for a specific venue and date
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.venueId = :venueId AND b.blockDate = :date AND b.isDeleted = false ORDER BY b.startTime")
    List<BlockedSlot> findByVenueAndDate(@Param("venueId") String venueId, @Param("date") LocalDate date);

    /**
     * Find all blocked slots for a specific venue (null = all venues)
     */
    @Query("SELECT b FROM BlockedSlot b WHERE (b.venueId = :venueId OR :venueId IS NULL) AND b.isDeleted = false ORDER BY b.blockDate, b.startTime")
    List<BlockedSlot> findByVenue(@Param("venueId") String venueId);

    /**
     * Find blocked slots within a date range for a venue (exact match)
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.venueId = :venueId AND b.blockDate >= :startDate AND b.blockDate <= :endDate AND b.isDeleted = false ORDER BY b.blockDate, b.startTime")
    List<BlockedSlot> findByVenueAndDateRange(@Param("venueId") String venueId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find blocked slots within a date range for a venue (includes null venueId = applies to all venues)
     */
    @Query("SELECT b FROM BlockedSlot b WHERE (b.venueId = :venueId OR b.venueId IS NULL) AND b.blockDate >= :startDate AND b.blockDate <= :endDate AND b.isDeleted = false ORDER BY b.blockDate, b.startTime")
    List<BlockedSlot> findBlockedSlotsForVenueAndDateRange(@Param("venueId") String venueId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find blocked slots within a date range (all venues)
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.blockDate >= :startDate AND b.blockDate <= :endDate AND b.isDeleted = false ORDER BY b.blockDate, b.startTime")
    List<BlockedSlot> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Check if a specific time slot is blocked for a venue on a date
     */
    @Query("SELECT COUNT(b) > 0 FROM BlockedSlot b WHERE " +
           "(b.venueId = :venueId OR b.venueId IS NULL) AND " +
           "b.blockDate = :date AND " +
           "b.isDeleted = false AND " +
           "(b.isFullDay = true OR (:time >= b.startTime AND (:time < b.endTime OR b.endTime IS NULL)))")
    boolean isSlotBlocked(@Param("venueId") String venueId, @Param("date") LocalDate date, @Param("time") LocalTime time);

    /**
     * Get blocked times for a specific venue and date
     */
    @Query("SELECT b FROM BlockedSlot b WHERE " +
           "(b.venueId = :venueId OR b.venueId IS NULL) AND " +
           "b.blockDate = :date AND " +
           "b.isDeleted = false")
    List<BlockedSlot> findBlockedTimesForVenueAndDate(@Param("venueId") String venueId, @Param("date") LocalDate date);

    /**
     * Find recurring blocked slots that apply to a date
     */
    @Query("SELECT b FROM BlockedSlot b WHERE " +
           "b.blockType != 'SINGLE' AND " +
           "b.blockDate <= :date AND " +
           "(b.recurringEndDate IS NULL OR b.recurringEndDate >= :date) AND " +
           "b.isDeleted = false")
    List<BlockedSlot> findRecurringBlocksForDate(@Param("date") LocalDate date);

    /**
     * Find by block type
     */
    @Query("SELECT b FROM BlockedSlot b WHERE b.blockType = :blockType AND b.isDeleted = false ORDER BY b.blockDate, b.startTime")
    List<BlockedSlot> findByBlockType(@Param("blockType") BlockType blockType);
}
