package com.mirror.product.dto.pod;

import com.mirror.product.entity.pod.InventoryMovement;
import com.mirror.product.enums.InventoryMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovementResponse {

    private String id;
    private String inventoryId;
    private String partnerId;
    private String productId;
    private InventoryMovementType movementType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String referenceId;
    private String referenceType;
    private String notes;
    private String createdBy;
    private Instant createdAt;

    public static InventoryMovementResponse fromEntity(InventoryMovement entity) {
        if (entity == null) return null;
        return InventoryMovementResponse.builder()
                .id(entity.getId())
                .inventoryId(entity.getInventoryId())
                .partnerId(entity.getPartnerId())
                .productId(entity.getProductId())
                .movementType(entity.getMovementType())
                .quantity(entity.getQuantity())
                .quantityBefore(entity.getQuantityBefore())
                .quantityAfter(entity.getQuantityAfter())
                .referenceId(entity.getReferenceId())
                .referenceType(entity.getReferenceType())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
