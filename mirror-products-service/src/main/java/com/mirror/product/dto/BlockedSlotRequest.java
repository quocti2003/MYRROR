package com.mirror.product.dto;

import com.mirror.product.enums.BlockType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSlotRequest {

    private String venueId;  // Optional - null means all venues

    @NotNull(message = "Block date is required")
    private LocalDate blockDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    private LocalTime endTime;  // Optional - if null, blocks single slot

    @Builder.Default
    private Boolean isFullDay = false;

    private String reason;

    @Builder.Default
    private BlockType blockType = BlockType.SINGLE;

    private LocalDate recurringEndDate;  // For recurring blocks

    private String createdBy;
}
