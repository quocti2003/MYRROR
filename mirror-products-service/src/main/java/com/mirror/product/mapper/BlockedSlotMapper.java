package com.mirror.product.mapper;

import com.mirror.product.dto.BlockedSlotRequest;
import com.mirror.product.dto.BlockedSlotResponse;
import com.mirror.product.entity.BlockedSlot;
import org.springframework.stereotype.Component;

@Component
public class BlockedSlotMapper {

    public BlockedSlot toEntity(BlockedSlotRequest request) {
        return BlockedSlot.builder()
                .venueId(request.getVenueId())
                .blockDate(request.getBlockDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isFullDay(request.getIsFullDay() != null ? request.getIsFullDay() : false)
                .reason(request.getReason())
                .blockType(request.getBlockType())
                .recurringEndDate(request.getRecurringEndDate())
                .createdBy(request.getCreatedBy())
                .build();
    }

    public BlockedSlotResponse toResponse(BlockedSlot entity) {
        BlockedSlotResponse.BlockedSlotResponseBuilder builder = BlockedSlotResponse.builder()
                .id(entity.getId())
                .venueId(entity.getVenueId())
                .blockDate(entity.getBlockDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .isFullDay(entity.getIsFullDay())
                .reason(entity.getReason())
                .blockType(entity.getBlockType())
                .recurringEndDate(entity.getRecurringEndDate())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // Add venue info if available
        if (entity.getVenue() != null) {
            builder.venueName(entity.getVenue().getName())
                   .venueCity(entity.getVenue().getCity());
        }

        return builder.build();
    }

    public void updateEntity(BlockedSlot entity, BlockedSlotRequest request) {
        if (request.getVenueId() != null) {
            entity.setVenueId(request.getVenueId());
        }
        if (request.getBlockDate() != null) {
            entity.setBlockDate(request.getBlockDate());
        }
        if (request.getStartTime() != null) {
            entity.setStartTime(request.getStartTime());
        }
        entity.setEndTime(request.getEndTime());
        if (request.getIsFullDay() != null) {
            entity.setIsFullDay(request.getIsFullDay());
        }
        if (request.getReason() != null) {
            entity.setReason(request.getReason());
        }
        if (request.getBlockType() != null) {
            entity.setBlockType(request.getBlockType());
        }
        entity.setRecurringEndDate(request.getRecurringEndDate());
    }
}
