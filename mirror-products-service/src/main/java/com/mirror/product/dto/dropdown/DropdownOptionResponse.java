package com.mirror.product.dto.dropdown;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response DTO for dropdown options
 * Used for all dropdown configuration types
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropdownOptionResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
}
