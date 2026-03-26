package com.mirror.product.service;

import com.mirror.product.dto.BlockedSlotRequest;
import com.mirror.product.dto.BlockedSlotResponse;
import com.mirror.product.entity.BlockedSlot;
import com.mirror.product.enums.BlockType;
import com.mirror.product.mapper.BlockedSlotMapper;
import com.mirror.product.repository.BlockedSlotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedSlotService {

    private final BlockedSlotRepository blockedSlotRepository;
    private final BlockedSlotMapper blockedSlotMapper;

    /**
     * Create a new blocked slot
     */
    @Transactional
    public BlockedSlotResponse create(BlockedSlotRequest request) {
        log.info("Creating blocked slot for venue: {} on date: {}", request.getVenueId(), request.getBlockDate());

        BlockedSlot blockedSlot = blockedSlotMapper.toEntity(request);
        BlockedSlot saved = blockedSlotRepository.save(blockedSlot);

        log.info("Created blocked slot: {}", saved.getId());
        return blockedSlotMapper.toResponse(saved);
    }

    /**
     * Get all blocked slots
     */
    public List<BlockedSlotResponse> getAll() {
        return blockedSlotRepository.findAllActive().stream()
                .map(blockedSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get blocked slot by ID
     */
    public BlockedSlotResponse getById(String id) {
        BlockedSlot blockedSlot = blockedSlotRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blocked slot not found: " + id));
        return blockedSlotMapper.toResponse(blockedSlot);
    }

    /**
     * Get blocked slots for a specific date
     */
    public List<BlockedSlotResponse> getByDate(LocalDate date) {
        List<BlockedSlot> slots = blockedSlotRepository.findByDate(date);

        // Also include recurring blocks that apply to this date
        List<BlockedSlot> recurringBlocks = getRecurringBlocksForDate(date);

        List<BlockedSlot> allBlocks = new ArrayList<>(slots);
        allBlocks.addAll(recurringBlocks);

        return allBlocks.stream()
                .map(blockedSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get blocked slots for a specific venue and date
     */
    public List<BlockedSlotResponse> getByVenueAndDate(String venueId, LocalDate date) {
        List<BlockedSlot> slots = blockedSlotRepository.findByVenueAndDate(venueId, date);

        // Also include recurring blocks that apply to this date
        List<BlockedSlot> recurringBlocks = getRecurringBlocksForDate(date).stream()
                .filter(b -> b.getVenueId() == null || b.getVenueId().equals(venueId))
                .collect(Collectors.toList());

        List<BlockedSlot> allBlocks = new ArrayList<>(slots);
        allBlocks.addAll(recurringBlocks);

        return allBlocks.stream()
                .map(blockedSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get blocked slots within a date range
     */
    public List<BlockedSlotResponse> getByDateRange(LocalDate startDate, LocalDate endDate, String venueId) {
        List<BlockedSlot> slots;
        if (venueId != null) {
            slots = blockedSlotRepository.findByVenueAndDateRange(venueId, startDate, endDate);
        } else {
            slots = blockedSlotRepository.findByDateRange(startDate, endDate);
        }

        return slots.stream()
                .map(blockedSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if a specific time slot is blocked
     */
    public boolean isSlotBlocked(String venueId, LocalDate date, LocalTime time) {
        // Check direct blocks
        boolean directlyBlocked = blockedSlotRepository.isSlotBlocked(venueId, date, time);
        if (directlyBlocked) {
            return true;
        }

        // Check recurring blocks
        List<BlockedSlot> recurringBlocks = getRecurringBlocksForDate(date);
        for (BlockedSlot block : recurringBlocks) {
            if (block.getVenueId() == null || block.getVenueId().equals(venueId)) {
                if (block.getIsFullDay()) {
                    return true;
                }
                if (time.compareTo(block.getStartTime()) >= 0) {
                    if (block.getEndTime() == null || time.compareTo(block.getEndTime()) < 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get blocked times for a specific venue and date (for slot availability calculation)
     */
    public List<LocalTime> getBlockedTimesForVenueAndDate(String venueId, LocalDate date) {
        List<BlockedSlot> blocks = blockedSlotRepository.findBlockedTimesForVenueAndDate(venueId, date);

        // Also include recurring blocks
        List<BlockedSlot> recurringBlocks = getRecurringBlocksForDate(date).stream()
                .filter(b -> b.getVenueId() == null || b.getVenueId().equals(venueId))
                .collect(Collectors.toList());

        List<BlockedSlot> allBlocks = new ArrayList<>(blocks);
        allBlocks.addAll(recurringBlocks);

        List<LocalTime> blockedTimes = new ArrayList<>();

        // Default time slots (9 AM to 6 PM)
        List<LocalTime> allSlots = List.of(
            LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(14, 0),
            LocalTime.of(15, 0), LocalTime.of(16, 0), LocalTime.of(17, 0),
            LocalTime.of(18, 0)
        );

        for (BlockedSlot block : allBlocks) {
            if (block.getIsFullDay()) {
                // All slots are blocked
                blockedTimes.addAll(allSlots);
            } else {
                // Check each slot against the block time range
                for (LocalTime slot : allSlots) {
                    if (slot.compareTo(block.getStartTime()) >= 0) {
                        if (block.getEndTime() == null || slot.compareTo(block.getEndTime()) < 0) {
                            if (!blockedTimes.contains(slot)) {
                                blockedTimes.add(slot);
                            }
                        }
                    }
                }
            }
        }

        return blockedTimes;
    }

    /**
     * Update a blocked slot
     */
    @Transactional
    public BlockedSlotResponse update(String id, BlockedSlotRequest request) {
        BlockedSlot blockedSlot = blockedSlotRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blocked slot not found: " + id));

        blockedSlotMapper.updateEntity(blockedSlot, request);
        BlockedSlot saved = blockedSlotRepository.save(blockedSlot);

        log.info("Updated blocked slot: {}", id);
        return blockedSlotMapper.toResponse(saved);
    }

    /**
     * Delete a blocked slot (soft delete)
     */
    @Transactional
    public void delete(String id) {
        BlockedSlot blockedSlot = blockedSlotRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blocked slot not found: " + id));

        blockedSlot.setIsDeleted(true);
        blockedSlot.setIsActive(false);
        blockedSlotRepository.save(blockedSlot);

        log.info("Deleted blocked slot: {}", id);
    }

    /**
     * Get recurring blocks that apply to a specific date
     */
    private List<BlockedSlot> getRecurringBlocksForDate(LocalDate date) {
        List<BlockedSlot> recurringBlocks = blockedSlotRepository.findRecurringBlocksForDate(date);
        List<BlockedSlot> applicableBlocks = new ArrayList<>();

        DayOfWeek targetDayOfWeek = date.getDayOfWeek();

        for (BlockedSlot block : recurringBlocks) {
            if (block.getBlockType() == BlockType.RECURRING_DAILY) {
                // Daily recurring - applies every day
                applicableBlocks.add(block);
            } else if (block.getBlockType() == BlockType.RECURRING_WEEKLY) {
                // Weekly recurring - only applies on same day of week
                if (block.getBlockDate().getDayOfWeek() == targetDayOfWeek) {
                    applicableBlocks.add(block);
                }
            }
        }

        return applicableBlocks;
    }

    /**
     * Get all full-day blocked dates for a venue within a date range (optimized batch query)
     * Returns dates where ALL time slots are blocked
     */
    public java.util.Set<LocalDate> getFullDayBlockedDatesForVenueAndDateRange(String venueId, LocalDate startDate, LocalDate endDate) {
        java.util.Set<LocalDate> fullyBlockedDates = new java.util.HashSet<>();

        // Get all blocked slots for the venue in the date range
        List<BlockedSlot> blockedSlots;
        if (venueId != null && !venueId.trim().isEmpty()) {
            blockedSlots = blockedSlotRepository.findByVenueAndDateRange(venueId, startDate, endDate);
        } else {
            blockedSlots = blockedSlotRepository.findByDateRange(startDate, endDate);
        }

        // Add dates with full-day blocks
        for (BlockedSlot slot : blockedSlots) {
            if (slot.getIsFullDay()) {
                fullyBlockedDates.add(slot.getBlockDate());
            }
        }

        // Also check recurring blocks
        List<BlockedSlot> recurringBlocks = blockedSlotRepository.findRecurringBlocksForDate(endDate);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            for (BlockedSlot block : recurringBlocks) {
                if (block.getVenueId() == null || block.getVenueId().equals(venueId)) {
                    if (block.getIsFullDay()) {
                        if (block.getBlockType() == BlockType.RECURRING_DAILY) {
                            fullyBlockedDates.add(date);
                        } else if (block.getBlockType() == BlockType.RECURRING_WEEKLY &&
                                   block.getBlockDate().getDayOfWeek() == dayOfWeek) {
                            fullyBlockedDates.add(date);
                        }
                    }
                }
            }
        }

        return fullyBlockedDates;
    }

    /**
     * Get blocked slots data for a date range (for batch processing)
     * Returns a map of date -> blocked times for that date
     */
    public java.util.Map<LocalDate, java.util.Set<LocalTime>> getBlockedTimesForVenueAndDateRange(String venueId, LocalDate startDate, LocalDate endDate) {
        java.util.Map<LocalDate, java.util.Set<LocalTime>> blockedTimesByDate = new java.util.HashMap<>();

        // Default time slots (9 AM to 6 PM)
        List<LocalTime> allSlots = List.of(
            LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(14, 0),
            LocalTime.of(15, 0), LocalTime.of(16, 0), LocalTime.of(17, 0),
            LocalTime.of(18, 0)
        );

        // Get all blocked slots for the venue in the date range (includes null venueId = applies to all)
        List<BlockedSlot> blockedSlots;
        if (venueId != null && !venueId.trim().isEmpty()) {
            blockedSlots = blockedSlotRepository.findBlockedSlotsForVenueAndDateRange(venueId, startDate, endDate);
        } else {
            blockedSlots = blockedSlotRepository.findByDateRange(startDate, endDate);
        }

        // Process blocked slots
        for (BlockedSlot block : blockedSlots) {
            LocalDate date = block.getBlockDate();
            blockedTimesByDate.computeIfAbsent(date, k -> new java.util.HashSet<>());

            if (block.getIsFullDay()) {
                blockedTimesByDate.get(date).addAll(allSlots);
            } else {
                for (LocalTime slot : allSlots) {
                    if (slot.compareTo(block.getStartTime()) >= 0) {
                        if (block.getEndTime() == null || slot.compareTo(block.getEndTime()) < 0) {
                            blockedTimesByDate.get(date).add(slot);
                        }
                    }
                }
            }
        }

        // Also check recurring blocks
        List<BlockedSlot> recurringBlocks = blockedSlotRepository.findRecurringBlocksForDate(endDate);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            for (BlockedSlot block : recurringBlocks) {
                if (block.getVenueId() == null || block.getVenueId().equals(venueId)) {
                    boolean applies = false;
                    if (block.getBlockType() == BlockType.RECURRING_DAILY) {
                        applies = true;
                    } else if (block.getBlockType() == BlockType.RECURRING_WEEKLY &&
                               block.getBlockDate().getDayOfWeek() == dayOfWeek) {
                        applies = true;
                    }

                    if (applies) {
                        LocalDate finalDate = date;
                        blockedTimesByDate.computeIfAbsent(finalDate, k -> new java.util.HashSet<>());

                        if (block.getIsFullDay()) {
                            blockedTimesByDate.get(date).addAll(allSlots);
                        } else {
                            for (LocalTime slot : allSlots) {
                                if (slot.compareTo(block.getStartTime()) >= 0) {
                                    if (block.getEndTime() == null || slot.compareTo(block.getEndTime()) < 0) {
                                        blockedTimesByDate.get(date).add(slot);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return blockedTimesByDate;
    }
}
