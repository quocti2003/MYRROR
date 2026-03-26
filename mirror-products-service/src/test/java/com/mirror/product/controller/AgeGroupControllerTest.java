package com.mirror.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.AgeGroupRequest;
import com.mirror.product.entity.AgeGroup;
import com.mirror.product.repository.AgeGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AgeGroupController
 * Tests CRUD operations, validation, and age group preferences
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-user", roles = {"USER"})
class AgeGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgeGroupRepository ageGroupRepository;

    @BeforeEach
    void setUp() {
        // Clean up
        ageGroupRepository.deleteAll();
    }

    @Test
    void shouldCreateAgeGroupSuccessfully() throws Exception {
        AgeGroupRequest request = AgeGroupRequest.builder()
                .name("Young Adults")
                .minAge(18)
                .maxAge(25)
                .build();

        mockMvc.perform(post("/api/age-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Young Adults"))
                .andExpect(jsonPath("$.minAge").value(18))
                .andExpect(jsonPath("$.maxAge").value(25))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturnValidationErrorForInvalidAgeRange() throws Exception {
        AgeGroupRequest request = AgeGroupRequest.builder()
                .name("Invalid Range")
                .minAge(30)
                .maxAge(20) // max < min
                .build();

        mockMvc.perform(post("/api/age-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Min age cannot be greater than max age")));
    }

    @Test
    void shouldReturnValidationErrorForMissingName() throws Exception {
        AgeGroupRequest request = AgeGroupRequest.builder()
                .minAge(18)
                .maxAge(25)
                // Missing name
                .build();

        mockMvc.perform(post("/api/age-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAllAgeGroups() throws Exception {
        // Create test age groups
        createTestAgeGroup("Young Adults", 18, 25);
        createTestAgeGroup("Adults", 26, 35);
        createTestAgeGroup("Mature Adults", 36, 50);

        mockMvc.perform(get("/api/age-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Young Adults", "Adults", "Mature Adults")));
    }

    @Test
    void shouldGetAllAgeGroupsWithPreferences() throws Exception {
        createTestAgeGroup("Test Group", 18, 25);

        mockMvc.perform(get("/api/age-groups?includePreferences=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Test Group"))
                .andExpect(jsonPath("$[0].preferences").exists());
    }

    @Test
    void shouldGetAgeGroupById() throws Exception {
        AgeGroup ageGroup = createTestAgeGroup("Test Group", 18, 25);

        mockMvc.perform(get("/api/age-groups/{id}", ageGroup.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ageGroup.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Group"))
                .andExpect(jsonPath("$.minAge").value(18))
                .andExpect(jsonPath("$.maxAge").value(25));
    }

    @Test
    void shouldReturnNotFoundForNonExistentAgeGroup() throws Exception {
        mockMvc.perform(get("/api/age-groups/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAgeGroupByName() throws Exception {
        createTestAgeGroup("Young Adults", 18, 25);

        mockMvc.perform(get("/api/age-groups/name/{name}", "Young Adults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Young Adults"))
                .andExpect(jsonPath("$.minAge").value(18));
    }

    @Test
    void shouldReturnNotFoundForNonExistentName() throws Exception {
        mockMvc.perform(get("/api/age-groups/name/{name}", "NonExistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateAgeGroupSuccessfully() throws Exception {
        AgeGroup ageGroup = createTestAgeGroup("Old Name", 18, 25);

        AgeGroupRequest updateRequest = AgeGroupRequest.builder()
                .name("Updated Name")
                .minAge(20)
                .maxAge(30)
                .build();

        mockMvc.perform(put("/api/age-groups/{id}", ageGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.minAge").value(20))
                .andExpect(jsonPath("$.maxAge").value(30));
    }

    @Test
    void shouldDeleteAgeGroupSuccessfully() throws Exception {
        AgeGroup ageGroup = createTestAgeGroup("To Delete", 18, 25);

        mockMvc.perform(delete("/api/age-groups/{id}", ageGroup.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Age group deleted successfully"));

        // Verify deletion
        mockMvc.perform(get("/api/age-groups/{id}", ageGroup.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAgeGroupCount() throws Exception {
        createTestAgeGroup("Group 1", 18, 25);
        createTestAgeGroup("Group 2", 26, 35);
        createTestAgeGroup("Group 3", 36, 50);

        mockMvc.perform(get("/api/age-groups/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void shouldReturnErrorWhenCreatingDuplicateAgeGroupName() throws Exception {
        createTestAgeGroup("Duplicate Name", 18, 25);

        AgeGroupRequest request = AgeGroupRequest.builder()
                .name("Duplicate Name")
                .minAge(26)
                .maxAge(35)
                .build();

        mockMvc.perform(post("/api/age-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("already exists")));
    }

    @Test
    void shouldHandleAgeGroupWithNoAgeRange() throws Exception {
        AgeGroupRequest request = AgeGroupRequest.builder()
                .name("All Ages")
                // No min/max age
                .build();

        mockMvc.perform(post("/api/age-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("All Ages"))
                .andExpect(jsonPath("$.minAge").doesNotExist())
                .andExpect(jsonPath("$.maxAge").doesNotExist());
    }

    // Helper method
    private AgeGroup createTestAgeGroup(String name, Integer minAge, Integer maxAge) {
        AgeGroup ageGroup = AgeGroup.builder()
                .name(name)
                .minAge(minAge)
                .maxAge(maxAge)
                .build();
        return ageGroupRepository.save(ageGroup);
    }
}
