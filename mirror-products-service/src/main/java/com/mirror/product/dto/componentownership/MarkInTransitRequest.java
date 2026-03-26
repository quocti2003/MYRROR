package com.mirror.product.dto.componentownership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for marking a handoff as in-transit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkInTransitRequest {

    /**
     * Tracking number for the shipment
     */
    private String trackingNumber;

    /**
     * Shipping carrier (e.g., "FedEx", "DHL", "Courier")
     */
    private String shippingCarrier;

    /**
     * Updated expected arrival date (optional)
     */
    private LocalDate expectedArrivalDate;

    /**
     * Additional notes about the shipment
     */
    private String notes;
}
