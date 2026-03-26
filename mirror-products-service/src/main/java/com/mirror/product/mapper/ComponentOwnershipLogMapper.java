package com.mirror.product.mapper;

import com.mirror.product.dto.componentownership.ComponentOwnershipLogDTO;
import com.mirror.product.entity.ComponentOwnershipLog;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Mapper for ComponentOwnershipLog entity and DTO
 */
@Component
public class ComponentOwnershipLogMapper {

    /**
     * Convert entity to DTO
     */
    public ComponentOwnershipLogDTO toDTO(ComponentOwnershipLog entity) {
        if (entity == null) {
            return null;
        }

        ComponentOwnershipLogDTO.ComponentOwnershipLogDTOBuilder builder = ComponentOwnershipLogDTO.builder()
                .id(entity.getId())
                .handoffType(entity.getHandoffType())
                .status(entity.getStatus())
                .initiatedBy(entity.getInitiatedBy())
                .initiatedAt(entity.getInitiatedAt())
                .receivedBy(entity.getReceivedBy())
                .receivedAt(entity.getReceivedAt())
                .expectedArrivalDate(entity.getExpectedArrivalDate())
                .trackingNumber(entity.getTrackingNumber())
                .shippingCarrier(entity.getShippingCarrier())
                .reason(entity.getReason())
                .notes(entity.getNotes())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // Production Order
        if (entity.getProductionOrder() != null) {
            builder.productionOrderId(entity.getProductionOrder().getId())
                   .productionOrderNumber(entity.getProductionOrder().getOrderNumber());
        }

        // Stage
        if (entity.getStage() != null) {
            builder.stageId(entity.getStage().getId())
                   .stageName(entity.getStage().getStageName());
        }

        // From Vendor
        if (entity.getFromVendor() != null) {
            builder.fromVendorId(entity.getFromVendor().getId())
                   .fromVendorName(entity.getFromVendor().getName());
        }

        // To Vendor
        if (entity.getToVendor() != null) {
            builder.toVendorId(entity.getToVendor().getId())
                   .toVendorName(entity.getToVendor().getName());
        }

        // Computed fields
        builder.canConfirmReceipt(entity.canConfirmReceipt())
               .canReject(entity.canReject())
               .canCancel(entity.canCancel())
               .isTerminal(entity.isTerminal());

        // Check if overdue
        boolean isOverdue = !entity.isTerminal() &&
                            entity.getExpectedArrivalDate() != null &&
                            entity.getExpectedArrivalDate().isBefore(LocalDate.now());
        builder.isOverdue(isOverdue);

        return builder.build();
    }
}
