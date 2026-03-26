package com.mirror.product.dto;

import com.mirror.product.enums.BlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSlotResponse {

    private String id;
    private String venueId;
    private String venueName;
    private String venueCity;
    private LocalDate blockDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isFullDay;
    private String reason;
    private BlockType blockType;
    private LocalDate recurringEndDate;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
