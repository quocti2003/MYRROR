package com.mirror.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.MaterialInventoryRequest;
import com.mirror.product.entity.MaterialInventory;
import com.mirror.product.entity.Vendor;
import com.mirror.product.repository.MaterialInventoryRepository;
import com.mirror.product.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MaterialInventoryController
 * Tests CRUD operations, validation, and API responses
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-user", roles = {"USER"})
class MaterialInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialInventoryRepository materialInventoryRepository;

    @Autowired
    private VendorRepository vendorRepository;

    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        // Clean up
        materialInventoryRepository.deleteAll();

        // Create test vendor
        testVendor = Vendor.builder()
                .id("TEST-VEN-001")
                .code("TESTVEN001")
                .name("Test Vendor")
                .country("USA")
                .isActive(true)
                .isDeleted(false)
                .build();
        testVendor = vendorRepository.save(testVendor);
    }

    @Test
    void shouldCreateMaterialSuccessfully() throws Exception {
        MaterialInventoryRequest request = MaterialInventoryRequest.builder()
                .name("18K White Gold")
                .type("METAL")
                .vendorId(testVendor.getId())
                .basePrice(new BigDecimal("85.50"))
                .marketPrice(new BigDecimal("90.00"))
                .currency("USD")
                .taxCustomsPercent(new BigDecimal("5.0"))
                .assemblyCostLocal(new BigDecimal("10.00"))
                .deliveryLeadTimeDays(7)
                .productionLeadTimeDays(14)
                .assemblyLeadTimeDays(3)
                .build();

        mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("18K White Gold"))
                .andExpect(jsonPath("$.type").value("METAL"))
                .andExpect(jsonPath("$.vendorId").value(testVendor.getId()))
                .andExpect(jsonPath("$.basePrice").value(85.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturnValidationErrorForInvalidMaterial() throws Exception {
        MaterialInventoryRequest request = MaterialInventoryRequest.builder()
                // Missing required fields: name, type, vendorId, currency
                .basePrice(new BigDecimal("85.50"))
                .build();

        mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAllMaterials() throws Exception {
        // Create test materials
        createTestMaterial("Gold", "METAL");
        createTestMaterial("Silver", "METAL");
        createTestMaterial("Diamond", "STONE");

        mockMvc.perform(get("/api/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Gold", "Silver", "Diamond")));
    }

    @Test
    void shouldGetMaterialById() throws Exception {
        MaterialInventory material = createTestMaterial("Test Material", "METAL");

        mockMvc.perform(get("/api/materials/{id}", material.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(material.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Material"))
                .andExpect(jsonPath("$.type").value("METAL"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentMaterial() throws Exception {
        mockMvc.perform(get("/api/materials/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetMaterialsByType() throws Exception {
        createTestMaterial("Gold", "METAL");
        createTestMaterial("Silver", "METAL");
        createTestMaterial("Diamond", "STONE");

        mockMvc.perform(get("/api/materials/type/{type}", "METAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].type", everyItem(is("METAL"))));
    }

    @Test
    void shouldGetMaterialsByVendor() throws Exception {
        createTestMaterial("Material 1", "METAL");
        createTestMaterial("Material 2", "STONE");

        mockMvc.perform(get("/api/materials/vendor/{vendorId}", testVendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].vendorId", everyItem(is(testVendor.getId()))));
    }

    @Test
    void shouldUpdateMaterialSuccessfully() throws Exception {
        MaterialInventory material = createTestMaterial("Old Name", "METAL");

        MaterialInventoryRequest updateRequest = MaterialInventoryRequest.builder()
                .name("Updated Name")
                .type("STONE")
                .vendorId(testVendor.getId())
                .basePrice(new BigDecimal("100.00"))
                .marketPrice(new BigDecimal("110.00"))
                .currency("USD")
                .build();

        mockMvc.perform(put("/api/materials/{id}", material.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.type").value("STONE"))
                .andExpect(jsonPath("$.basePrice").value(100.00));
    }

    @Test
    void shouldDeleteMaterialSuccessfully() throws Exception {
        MaterialInventory material = createTestMaterial("To Delete", "METAL");

        mockMvc.perform(delete("/api/materials/{id}", material.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Material deleted successfully"));

        // Verify deletion
        mockMvc.perform(get("/api/materials/{id}", material.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnMaterialCount() throws Exception {
        createTestMaterial("Material 1", "METAL");
        createTestMaterial("Material 2", "STONE");
        createTestMaterial("Material 3", "METAL");

        mockMvc.perform(get("/api/materials/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void shouldReturnErrorWhenCreatingDuplicateMaterialName() throws Exception {
        createTestMaterial("Duplicate Name", "METAL");

        MaterialInventoryRequest request = MaterialInventoryRequest.builder()
                .name("Duplicate Name")
                .type("STONE")
                .vendorId(testVendor.getId())
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("already exists")));
    }

    // Helper method
    private MaterialInventory createTestMaterial(String name, String type) {
        MaterialInventory material = MaterialInventory.builder()
                .name(name)
                .type(type)
                .vendor(testVendor)
                .basePrice(new BigDecimal("50.00"))
                .marketPrice(new BigDecimal("55.00"))
                .currency("USD")
                .build();
        return materialInventoryRepository.save(material);
    }
}
