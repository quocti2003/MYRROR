package com.mirror.product.dto.pod;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodUpdateRequest {

    @Size(max = 255, message = "POD name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 255, message = "Location name must not exceed 255 characters")
    private String locationName;

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Min(value = 1, message = "Display capacity must be at least 1")
    @Max(value = 100, message = "Display capacity must not exceed 100")
    private Integer displayCapacity;

    /**
     * Optional: Commission rate for this POD (0-100%).
     * If not set, uses Partner's commission rate.
     */
    @DecimalMin(value = "0.0", message = "Commission rate must be at least 0")
    @DecimalMax(value = "100.0", message = "Commission rate must not exceed 100")
    private BigDecimal commissionRate;

    private LocalDate installationDate;

    private LocalDate lastMaintenanceDate;

    private LocalDate nextMaintenanceDate;

    private String notes;
}
