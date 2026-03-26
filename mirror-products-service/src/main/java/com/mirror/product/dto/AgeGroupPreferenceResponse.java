package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.AgeGroupPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Age Group Preference
 * Migrated from mirror-mrp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgeGroupPreferenceResponse {

    private UUID id;
    private UUID ageGroupId;
    private String ageGroupName;
    private String attributeType;
    private List<String> preferredValues;
    private Integer weight;

    public AgeGroupPreferenceResponse(AgeGroupPreference preference) {
        this.id = preference.getId();
        this.attributeType = preference.getAttributeType();
        this.preferredValues = preference.getPreferredValues();
        this.weight = preference.getWeight();

        if (preference.getAgeGroup() != null) {
            this.ageGroupId = preference.getAgeGroup().getId();
            this.ageGroupName = preference.getAgeGroup().getName();
        }
    }
}
