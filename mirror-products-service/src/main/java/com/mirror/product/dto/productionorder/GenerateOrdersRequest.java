package com.mirror.product.dto.productionorder;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for generating orders from a production plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateOrdersRequest {

    // Optional: Generate orders only for specific collection plan items
    private List<UUID> collectionPlanItemIds;

    // Optional: Generate orders for specific JTRCs
    private List<String> jtrcIds;

    // Optional: Override estimated completion date
    private LocalDate estimatedCompletionDate;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
