package com.mirror.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.dto.PurchaseOrderItemRequest;
import com.mirror.product.dto.PurchaseOrderRequest;
import com.mirror.product.entity.MaterialInventory;
import com.mirror.product.entity.PurchaseOrder;
import com.mirror.product.entity.Vendor;
import com.mirror.product.repository.MaterialInventoryRepository;
import com.mirror.product.repository.PurchaseOrderRepository;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PurchaseOrderController
 * Tests CRUD operations, status management, and complex PO scenarios
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-user", roles = {"USER"})
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MaterialInventoryRepository materialInventoryRepository;

    private Vendor testVendor;
    private MaterialInventory testMaterial1;
    private MaterialInventory testMaterial2;

    @BeforeEach
    void setUp() {
        // Clean up
        purchaseOrderRepository.deleteAll();
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

        // Create test materials
        testMaterial1 = MaterialInventory.builder()
                .name("Gold 18K")
                .type("METAL")
                .vendor(testVendor)
                .basePrice(new BigDecimal("85.50"))
                .marketPrice(new BigDecimal("90.00"))
                .currency("USD")
                .build();
        testMaterial1 = materialInventoryRepository.save(testMaterial1);

        testMaterial2 = MaterialInventory.builder()
                .name("Diamond 1ct")
                .type("STONE")
                .vendor(testVendor)
                .basePrice(new BigDecimal("500.00"))
                .marketPrice(new BigDecimal("550.00"))
                .currency("USD")
                .build();
        testMaterial2 = materialInventoryRepository.save(testMaterial2);
    }

    @Test
    void shouldCreatePurchaseOrderSuccessfully() throws Exception {
        PurchaseOrderItemRequest item1 = PurchaseOrderItemRequest.builder()
                .materialId(testMaterial1.getId())
                .quantity(100)
                .unitPrice(new BigDecimal("85.50"))
                .build();

        PurchaseOrderItemRequest item2 = PurchaseOrderItemRequest.builder()
                .materialId(testMaterial2.getId())
                .quantity(50)
                .unitPrice(new BigDecimal("500.00"))
                .build();

        PurchaseOrderRequest request = PurchaseOrderRequest.builder()
                .vendorId(testVendor.getId())
                .status("DRAFT")
                .expectedDeliveryDate(LocalDate.now().plusDays(30))
                .items(Arrays.asList(item1, item2))
                .build();

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorId").value(testVendor.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.totalCost").value(33550.00)) // (100*85.50) + (50*500)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void shouldReturnValidationErrorForEmptyItems() throws Exception {
        PurchaseOrderRequest request = PurchaseOrderRequest.builder()
                .vendorId(testVendor.getId())
                .status("DRAFT")
                .items(Arrays.asList()) // Empty items
                .build();

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnValidationErrorForMissingVendor() throws Exception {
        PurchaseOrderItemRequest item = PurchaseOrderItemRequest.builder()
                .materialId(testMaterial1.getId())
                .quantity(100)
                .unitPrice(new BigDecimal("85.50"))
                .build();

        PurchaseOrderRequest request = PurchaseOrderRequest.builder()
                // Missing vendorId
                .status("DRAFT")
                .items(Arrays.asList(item))
                .build();

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAllPurchaseOrders() throws Exception {
        createTestPurchaseOrder("DRAFT");
        createTestPurchaseOrder("SENT");
        createTestPurchaseOrder("CONFIRMED");

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", containsInAnyOrder("DRAFT", "SENT", "CONFIRMED")));
    }

    @Test
    void shouldGetPurchaseOrderById() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("DRAFT");

        mockMvc.perform(get("/api/purchase-orders/{id}?includeItems=true", po.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(po.getId().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items").exists());
    }

    @Test
    void shouldGetPurchaseOrdersByStatus() throws Exception {
        createTestPurchaseOrder("DRAFT");
        createTestPurchaseOrder("DRAFT");
        createTestPurchaseOrder("SENT");

        mockMvc.perform(get("/api/purchase-orders/status/{status}", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", everyItem(is("DRAFT"))));
    }

    @Test
    void shouldGetPurchaseOrdersByVendor() throws Exception {
        createTestPurchaseOrder("DRAFT");
        createTestPurchaseOrder("SENT");

        mockMvc.perform(get("/api/purchase-orders/vendor/{vendorId}", testVendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].vendorId", everyItem(is(testVendor.getId()))));
    }

    @Test
    void shouldGetPurchaseOrdersByVendorAndStatus() throws Exception {
        createTestPurchaseOrder("DRAFT");
        createTestPurchaseOrder("SENT");

        mockMvc.perform(get("/api/purchase-orders/vendor/{vendorId}?status=DRAFT", testVendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void shouldUpdatePurchaseOrderStatus() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("DRAFT");

        mockMvc.perform(patch("/api/purchase-orders/{id}/status", po.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void shouldCancelPurchaseOrder() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("DRAFT");

        mockMvc.perform(post("/api/purchase-orders/{id}/cancel", po.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldNotCancelReceivedPurchaseOrder() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("RECEIVED");

        mockMvc.perform(post("/api/purchase-orders/{id}/cancel", po.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Cannot cancel")));
    }

    @Test
    void shouldDeleteDraftPurchaseOrder() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("DRAFT");

        mockMvc.perform(delete("/api/purchase-orders/{id}", po.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Purchase order deleted successfully"));
    }

    @Test
    void shouldNotDeleteNonDraftPurchaseOrder() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("SENT");

        mockMvc.perform(delete("/api/purchase-orders/{id}", po.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Only DRAFT or CANCELLED")));
    }

    @Test
    void shouldUpdatePurchaseOrderSuccessfully() throws Exception {
        PurchaseOrder po = createTestPurchaseOrder("DRAFT");

        PurchaseOrderItemRequest newItem = PurchaseOrderItemRequest.builder()
                .materialId(testMaterial2.getId())
                .quantity(75)
                .unitPrice(new BigDecimal("500.00"))
                .build();

        PurchaseOrderRequest updateRequest = PurchaseOrderRequest.builder()
                .vendorId(testVendor.getId())
                .status("SENT")
                .expectedDeliveryDate(LocalDate.now().plusDays(45))
                .items(Arrays.asList(newItem))
                .build();

        mockMvc.perform(put("/api/purchase-orders/{id}", po.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.totalCost").value(37500.00)); // 75 * 500
    }

    @Test
    void shouldReturnPurchaseOrderCount() throws Exception {
        createTestPurchaseOrder("DRAFT");
        createTestPurchaseOrder("SENT");
        createTestPurchaseOrder("CONFIRMED");

        mockMvc.perform(get("/api/purchase-orders/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void shouldGetPurchaseOrdersByDeliveryDateRange() throws Exception {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(60);

        createTestPurchaseOrder("DRAFT");

        mockMvc.perform(get("/api/purchase-orders/delivery-date?start={start}&end={end}", start, end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // Helper method
    private PurchaseOrder createTestPurchaseOrder(String status) {
        PurchaseOrder po = PurchaseOrder.builder()
                .vendor(testVendor)
                .status(status)
                .expectedDeliveryDate(LocalDate.now().plusDays(30))
                .totalCost(new BigDecimal("1000.00"))
                .build();
        return purchaseOrderRepository.save(po);
    }
}
