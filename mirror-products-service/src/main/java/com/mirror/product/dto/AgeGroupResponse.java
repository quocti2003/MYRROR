package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.AgeGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Age Group
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgeGroupResponse {

    private UUID id;
    private String name;
    private Integer minAge;
    private Integer maxAge;
    private Integer preferenceCount;
    private Integer allocationCount;
    private List<AgeGroupPreferenceResponse> preferences;

    public AgeGroupResponse(AgeGroup ageGroup) {
        this.id = ageGroup.getId();
        this.name = ageGroup.getName();
        this.minAge = ageGroup.getMinAge();
        this.maxAge = ageGroup.getMaxAge();

        if (ageGroup.getAgeGroupPreferences() != null) {
            this.preferenceCount = ageGroup.getAgeGroupPreferences().size();
            this.preferences = ageGroup.getAgeGroupPreferences().stream()
                    .map(AgeGroupPreferenceResponse::new)
                    .collect(Collectors.toList());
        }

        if (ageGroup.getPurchaseOrderAgeGroupAllocations() != null) {
            this.allocationCount = ageGroup.getPurchaseOrderAgeGroupAllocations().size();
        }
    }

    public AgeGroupResponse(AgeGroup ageGroup, boolean includePreferences) {
        this.id = ageGroup.getId();
        this.name = ageGroup.getName();
        this.minAge = ageGroup.getMinAge();
        this.maxAge = ageGroup.getMaxAge();

        if (ageGroup.getAgeGroupPreferences() != null) {
            this.preferenceCount = ageGroup.getAgeGroupPreferences().size();
        } else {
            this.preferenceCount = 0;
        }

        if (includePreferences) {
            this.preferences = ageGroup.getAgeGroupPreferences() != null
                    ? ageGroup.getAgeGroupPreferences().stream()
                            .map(AgeGroupPreferenceResponse::new)
                            .collect(Collectors.toList())
                    : List.of();
        }

        if (ageGroup.getPurchaseOrderAgeGroupAllocations() != null) {
            this.allocationCount = ageGroup.getPurchaseOrderAgeGroupAllocations().size();
        }
    }
}
