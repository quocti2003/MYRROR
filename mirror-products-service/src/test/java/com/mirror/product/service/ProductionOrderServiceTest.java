package com.mirror.product.service;

import com.mirror.product.dto.productionorder.*;
import com.mirror.product.entity.*;
import com.mirror.product.enums.*;
import com.mirror.product.mapper.ProductionOrderMapper;
import com.mirror.product.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ProductionOrderService
 * Covers: CRUD, Stage Lifecycle, Order Generation, Status Transitions
 */
@ExtendWith(MockitoExtension.class)
class ProductionOrderServiceTest {

    @Mock
    private ProductionOrderRepository orderRepository;

    @Mock
    private ProductionOrderStageRepository stageRepository;

    @Mock
    private ProductionPlanRepository planRepository;

    @Mock
    private WorkflowTemplateRepository templateRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private JewelryTechnicalReportRepository jtrcRepository;

    @Mock
    private CollectionPlanItemRepository collectionPlanItemRepository;

    @Mock
    private PartnerCapabilityRepository capabilityRepository;

    @Mock
    private ComponentOwnershipLogRepository ownershipLogRepository;

    @Mock
    private ProductionOrderMapper mapper;

    @Mock
    private WorkflowTemplateService workflowTemplateService;

    @Mock
    private JTRCProductLinkService jtrcProductLinkService;

    @InjectMocks
    private ProductionOrderService orderService;

    // Test data
    private ProductionPlan testPlan;
    private WorkflowTemplate testTemplate;
    private ProductionOrder testOrder;
    private ProductionOrderStage testStage1;
    private ProductionOrderStage testStage2;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        // Set up test template with stages
        testTemplate = WorkflowTemplate.builder()
                .id("template-1")
                .name("Test Template")
                .status(WorkflowTemplateStatus.ACTIVE)
                .stages(new ArrayList<>())
                .build();

        WorkflowStage templateStage1 = WorkflowStage.builder()
                .id("ws-1")
                .template(testTemplate)
                .stageOrder(1)
                .name("Stage 1")
                .requiredCapability(PartnerCapabilityType.STONE_SOURCING)
                .estimatedDurationDays(5)
                .isFinalStage(false)
                .build();

        WorkflowStage templateStage2 = WorkflowStage.builder()
                .id("ws-2")
                .template(testTemplate)
                .stageOrder(2)
                .name("Stage 2")
                .requiredCapability(PartnerCapabilityType.SETTING)
                .estimatedDurationDays(3)
                .isFinalStage(true)
                .build();

        testTemplate.getStages().add(templateStage1);
        testTemplate.getStages().add(templateStage2);

        // Set up test plan
        testPlan = ProductionPlan.builder()
                .id("plan-1")
                .name("Test Plan")
                .workflowTemplate(testTemplate)
                .status(ProductionPlanStatus.APPROVED)
                .build();

        // Set up test order with stages
        testOrder = ProductionOrder.builder()
                .id("order-1")
                .orderNumber("PO-2026-02-0001")
                .productionPlan(testPlan)
                .status(ProductionOrderStatus.READY)
                .quantity(1)
                .currentStageOrder(1)
                .stages(new ArrayList<>())
                .build();

        testStage1 = ProductionOrderStage.builder()
                .id("stage-1")
                .productionOrder(testOrder)
                .workflowStage(templateStage1)
                .stageOrder(1)
                .stageName("Stage 1")
                .requiredCapability(PartnerCapabilityType.STONE_SOURCING)
                .status(ProductionOrderStageStatus.READY)
                .isFinalStage(false)
                .build();

        testStage2 = ProductionOrderStage.builder()
                .id("stage-2")
                .productionOrder(testOrder)
                .workflowStage(templateStage2)
                .stageOrder(2)
                .stageName("Stage 2")
                .requiredCapability(PartnerCapabilityType.SETTING)
                .status(ProductionOrderStageStatus.BLOCKED)
                .isFinalStage(true)
                .build();

        testOrder.getStages().add(testStage1);
        testOrder.getStages().add(testStage2);

        // Set up test vendor
        testVendor = Vendor.builder()
                .id("vendor-1")
                .name("Test Vendor")
                .build();
    }

    // ==================== Find Operations Tests ====================

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("findById returns order when exists")
        void findById_whenExists_returnsOrder() {
            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));

            Optional<ProductionOrder> result = orderService.findById("order-1");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("order-1");
        }

        @Test
        @DisplayName("findById returns empty when not exists")
        void findById_whenNotExists_returnsEmpty() {
            when(orderRepository.findActiveById("non-existent")).thenReturn(Optional.empty());

            Optional<ProductionOrder> result = orderService.findById("non-existent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findByOrderNumber returns order when exists")
        void findByOrderNumber_whenExists_returnsOrder() {
            when(orderRepository.findActiveByOrderNumber("PO-2026-02-0001"))
                    .thenReturn(Optional.of(testOrder));

            Optional<ProductionOrder> result = orderService.findByOrderNumber("PO-2026-02-0001");

            assertThat(result).isPresent();
            assertThat(result.get().getOrderNumber()).isEqualTo("PO-2026-02-0001");
        }
    }

    // ==================== Create Operations Tests ====================

    @Nested
    @DisplayName("Create Operations")
    class CreateOperations {

        @Test
        @DisplayName("create with valid request creates order with stages")
        void create_withValidRequest_createsOrderWithStages() {
            ProductionOrderCreateRequest request = ProductionOrderCreateRequest.builder()
                    .productionPlanId("plan-1")
                    .quantity(1)
                    .build();

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));
            when(mapper.toEntity(any(), any())).thenReturn(testOrder);
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.createStageFromTemplate(any(), any()))
                    .thenReturn(testStage1)
                    .thenReturn(testStage2);
            when(orderRepository.findLastOrderNumberWithPrefix(anyString()))
                    .thenReturn(Optional.empty());

            ProductionOrder result = orderService.create(request, "user-1");

            assertThat(result).isNotNull();
            verify(orderRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("create with invalid plan ID throws exception")
        void create_withInvalidPlanId_throwsException() {
            ProductionOrderCreateRequest request = ProductionOrderCreateRequest.builder()
                    .productionPlanId("invalid-plan")
                    .build();

            when(planRepository.findActiveByIdWithWorkflowTemplate("invalid-plan"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(request, "user-1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("create with JTRC link links JTRC to order")
        void create_withJtrcLink_linksJtrcToOrder() {
            JewelryTechnicalReport jtrc = JewelryTechnicalReport.builder()
                    .id("jtrc-1")
                    .reportNumber("JTRC-2026-001")
                    .build();

            ProductionOrderCreateRequest request = ProductionOrderCreateRequest.builder()
                    .productionPlanId("plan-1")
                    .jtrcId("jtrc-1")
                    .build();

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));
            when(jtrcRepository.findActiveById("jtrc-1"))
                    .thenReturn(Optional.of(jtrc));
            when(mapper.toEntity(any(), any())).thenReturn(testOrder);
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.createStageFromTemplate(any(), any()))
                    .thenReturn(testStage1)
                    .thenReturn(testStage2);
            when(orderRepository.findLastOrderNumberWithPrefix(anyString()))
                    .thenReturn(Optional.empty());

            ProductionOrder result = orderService.create(request, "user-1");

            assertThat(result.getJtrc()).isEqualTo(jtrc);
        }
    }

    // ==================== Update Operations Tests ====================

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {

        @Test
        @DisplayName("update draft order updates fields")
        void update_draftOrder_updatesFields() {
            testOrder.setStatus(ProductionOrderStatus.DRAFT);
            ProductionOrderUpdateRequest request = ProductionOrderUpdateRequest.builder()
                    .notes("Updated notes")
                    .build();

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.update("order-1", request, "user-1");

            assertThat(result).isNotNull();
            verify(mapper).updateEntity(eq(testOrder), eq(request));
        }

        @Test
        @DisplayName("update IN_PROGRESS order throws exception")
        void update_inProgressOrder_throwsException() {
            testOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);
            ProductionOrderUpdateRequest request = ProductionOrderUpdateRequest.builder()
                    .notes("Updated notes")
                    .build();

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.update("order-1", request, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot update order");
        }

        @Test
        @DisplayName("updateStatus with valid transition updates status")
        void updateStatus_validTransition_updatesStatus() {
            testOrder.setStatus(ProductionOrderStatus.DRAFT);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.updateStatus("order-1",
                    ProductionOrderStatus.READY, "user-1", null);

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStatus.READY);
        }

        @Test
        @DisplayName("updateStatus with invalid transition throws exception")
        void updateStatus_invalidTransition_throwsException() {
            testOrder.setStatus(ProductionOrderStatus.COMPLETED);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateStatus("order-1",
                    ProductionOrderStatus.DRAFT, "user-1", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("updateStatus to COMPLETED sets actualCompletionDate")
        void updateStatus_toCompleted_setsCompletionDate() {
            testOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.updateStatus("order-1",
                    ProductionOrderStatus.COMPLETED, "user-1", null);

            assertThat(result.getActualCompletionDate()).isEqualTo(LocalDate.now());
        }
    }

    // ==================== Delete Operations Tests ====================

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("softDelete draft order marks as deleted")
        void softDelete_draftOrder_marksDeleted() {
            testOrder.setStatus(ProductionOrderStatus.DRAFT);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.softDelete("order-1", "user-1");

            assertThat(testOrder.getIsDeleted()).isTrue();
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("softDelete IN_PROGRESS order throws exception")
        void softDelete_inProgressOrder_throwsException() {
            testOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.softDelete("order-1", "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete order");
        }

        @Test
        @DisplayName("softDelete non-existent order throws exception")
        void softDelete_nonExistentOrder_throwsException() {
            when(orderRepository.findActiveById("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.softDelete("non-existent", "user-1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ==================== Stage Lifecycle Tests ====================

    @Nested
    @DisplayName("Stage Lifecycle Operations")
    class StageLifecycle {

        @Test
        @DisplayName("startStage with READY stage sets IN_PROGRESS")
        void startStage_readyStage_setsInProgress() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            doNothing().when(workflowTemplateService).validateCanStartStage(any());
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrderStage result = orderService.startStage("order-1", "stage-1", "user-1");

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStageStatus.IN_PROGRESS);
            assertThat(result.getActualStartDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("startStage with BLOCKED stage throws exception")
        void startStage_blockedStage_throwsException() {
            testStage1.setStatus(ProductionOrderStageStatus.BLOCKED);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            doThrow(new IllegalStateException("Cannot start stage in status: BLOCKED"))
                    .when(workflowTemplateService).validateCanStartStage(ProductionOrderStageStatus.BLOCKED);

            assertThatThrownBy(() -> orderService.startStage("order-1", "stage-1", "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BLOCKED");
        }

        @Test
        @DisplayName("startStage updates order status to IN_PROGRESS")
        void startStage_updatesOrderStatus() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);
            testOrder.setStatus(ProductionOrderStatus.READY);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            doNothing().when(workflowTemplateService).validateCanStartStage(any());
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.startStage("order-1", "stage-1", "user-1");

            assertThat(testOrder.getStatus()).isEqualTo(ProductionOrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("startStage with wrong order ID throws exception")
        void startStage_wrongOrderId_throwsException() {
            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));

            assertThatThrownBy(() -> orderService.startStage("wrong-order", "stage-1", "user-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("completeStage with IN_PROGRESS stage sets COMPLETED")
        void completeStage_inProgressStage_setsCompleted() {
            testStage1.setStatus(ProductionOrderStageStatus.IN_PROGRESS);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            doNothing().when(workflowTemplateService).validateCanCompleteStage(any());
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(stageRepository.findNextStage(anyString(), anyInt()))
                    .thenReturn(Optional.of(testStage2));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            StageCompleteRequest request = StageCompleteRequest.builder()
                    .actualCost(BigDecimal.valueOf(500000))
                    .notes("Completed")
                    .build();

            ProductionOrderStage result = orderService.completeStage("order-1", "stage-1", request, "user-1");

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStageStatus.COMPLETED);
            assertThat(result.getActualEndDate()).isEqualTo(LocalDate.now());
            assertThat(result.getCompletedBy()).isEqualTo("user-1");
            assertThat(result.getActualCost()).isEqualTo(BigDecimal.valueOf(500000));
        }

        @Test
        @DisplayName("completeStage unlocks next stage")
        void completeStage_unlocksNextStage() {
            testStage1.setStatus(ProductionOrderStageStatus.IN_PROGRESS);
            testStage2.setStatus(ProductionOrderStageStatus.BLOCKED);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            doNothing().when(workflowTemplateService).validateCanCompleteStage(any());
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(stageRepository.findNextStage("order-1", 1))
                    .thenReturn(Optional.of(testStage2));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.completeStage("order-1", "stage-1", null, "user-1");

            assertThat(testStage2.getStatus()).isEqualTo(ProductionOrderStageStatus.READY);
        }

        @Test
        @DisplayName("completeStage on final stage completes order")
        void completeStage_finalStage_completesOrder() {
            testStage2.setStatus(ProductionOrderStageStatus.IN_PROGRESS);
            testStage2.setIsFinalStage(true);
            testOrder.setCurrentStageOrder(2);

            when(stageRepository.findActiveByIdWithOrder("stage-2"))
                    .thenReturn(Optional.of(testStage2));
            doNothing().when(workflowTemplateService).validateCanCompleteStage(any());
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(stageRepository.findNextStage("order-1", 2))
                    .thenReturn(Optional.empty()); // No next stage
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.completeStage("order-1", "stage-2", null, "user-1");

            assertThat(testOrder.getStatus()).isEqualTo(ProductionOrderStatus.COMPLETED);
            assertThat(testOrder.getActualCompletionDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("skipStage with BLOCKED/READY stage sets SKIPPED")
        void skipStage_blockedOrReady_setsSkipped() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(stageRepository.findNextStage(anyString(), anyInt()))
                    .thenReturn(Optional.of(testStage2));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrderStage result = orderService.skipStage("order-1", "stage-1", "user-1", "Not needed");

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStageStatus.SKIPPED);
            assertThat(result.getNotes()).isEqualTo("Not needed");
        }

        @Test
        @DisplayName("skipStage with IN_PROGRESS stage throws exception")
        void skipStage_inProgress_throwsException() {
            testStage1.setStatus(ProductionOrderStageStatus.IN_PROGRESS);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));

            assertThatThrownBy(() -> orderService.skipStage("order-1", "stage-1", "user-1", "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be skipped");
        }

        @Test
        @DisplayName("skipStage on final stage throws exception")
        void skipStage_finalStage_throwsException() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);
            testStage1.setIsFinalStage(true);

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));

            assertThatThrownBy(() -> orderService.skipStage("order-1", "stage-1", "user-1", "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot skip the final stage");
        }

        @Test
        @DisplayName("assignVendor to valid stage assigns vendor")
        void assignVendor_validStage_assignsVendor() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);
            testStage1.setRequiredCapability(PartnerCapabilityType.STONE_POLISHING);
            StageAssignVendorRequest request = StageAssignVendorRequest.builder()
                    .vendorId("vendor-1")
                    .estimatedCost(BigDecimal.valueOf(1000000))
                    .build();

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            when(vendorRepository.findById("vendor-1"))
                    .thenReturn(Optional.of(testVendor));
            when(capabilityRepository.existsActiveByVendorIdAndCapabilityType("vendor-1", PartnerCapabilityType.STONE_POLISHING))
                    .thenReturn(true);
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrderStage result = orderService.assignVendorToStage("order-1", "stage-1", request, "user-1");

            assertThat(result.getAssignedVendor()).isEqualTo(testVendor);
            assertThat(result.getEstimatedCost()).isEqualTo(BigDecimal.valueOf(1000000));
            verify(capabilityRepository).existsActiveByVendorIdAndCapabilityType("vendor-1", PartnerCapabilityType.STONE_POLISHING);
        }

        @Test
        @DisplayName("assignVendor without required capability throws exception")
        void assignVendor_withoutCapability_throwsException() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);
            testStage1.setRequiredCapability(PartnerCapabilityType.STONE_POLISHING);
            StageAssignVendorRequest request = StageAssignVendorRequest.builder()
                    .vendorId("vendor-1")
                    .build();

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            when(vendorRepository.findById("vendor-1"))
                    .thenReturn(Optional.of(testVendor));
            when(capabilityRepository.existsActiveByVendorIdAndCapabilityType("vendor-1", PartnerCapabilityType.STONE_POLISHING))
                    .thenReturn(false);

            assertThatThrownBy(() -> orderService.assignVendorToStage("order-1", "stage-1", request, "user-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not have the required capability");
        }

        @Test
        @DisplayName("assignVendor to stage without required capability succeeds")
        void assignVendor_noRequiredCapability_succeeds() {
            testStage1.setStatus(ProductionOrderStageStatus.READY);
            testStage1.setRequiredCapability(null); // No required capability
            StageAssignVendorRequest request = StageAssignVendorRequest.builder()
                    .vendorId("vendor-1")
                    .build();

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));
            when(vendorRepository.findById("vendor-1"))
                    .thenReturn(Optional.of(testVendor));
            when(stageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrderStage result = orderService.assignVendorToStage("order-1", "stage-1", request, "user-1");

            assertThat(result.getAssignedVendor()).isEqualTo(testVendor);
            verify(capabilityRepository, never()).existsActiveByVendorIdAndCapabilityType(any(), any());
        }

        @Test
        @DisplayName("assignVendor to completed stage throws exception")
        void assignVendor_completedStage_throwsException() {
            testStage1.setStatus(ProductionOrderStageStatus.COMPLETED);
            StageAssignVendorRequest request = StageAssignVendorRequest.builder()
                    .vendorId("vendor-1")
                    .build();

            when(stageRepository.findActiveByIdWithOrder("stage-1"))
                    .thenReturn(Optional.of(testStage1));

            assertThatThrownBy(() -> orderService.assignVendorToStage("order-1", "stage-1", request, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("completed or skipped");
        }
    }

    // ==================== Generate Orders Tests ====================

    @Nested
    @DisplayName("Generate Orders from Plan")
    class GenerateOrders {

        @Test
        @DisplayName("generateOrders from APPROVED plan creates orders")
        void generateOrders_fromApprovedPlan_createsOrders() {
            testPlan.setStatus(ProductionPlanStatus.APPROVED);

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));
            when(templateRepository.findActiveByIdWithStages(any()))
                    .thenReturn(Optional.of(testTemplate));
            when(orderRepository.save(any())).thenAnswer(i -> {
                ProductionOrder o = i.getArgument(0);
                if (o.getId() == null) o.setId("new-order");
                if (o.getStages() == null) o.setStages(new ArrayList<>());
                return o;
            });
            when(mapper.createStageFromTemplate(any(), any()))
                    .thenAnswer(i -> {
                        WorkflowStage ws = i.getArgument(0);
                        ProductionOrder o = i.getArgument(1);
                        return ProductionOrderStage.builder()
                                .id("gen-stage-" + ws.getStageOrder())
                                .stageOrder(ws.getStageOrder())
                                .stageName(ws.getName())
                                .status(ws.getStageOrder() == 1
                                        ? ProductionOrderStageStatus.READY
                                        : ProductionOrderStageStatus.BLOCKED)
                                .isFinalStage(ws.getIsFinalStage())
                                .build();
                    });
            when(orderRepository.findLastOrderNumberWithPrefix(anyString()))
                    .thenReturn(Optional.empty());
            when(planRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toListResponseList(any())).thenReturn(List.of());

            GenerateOrdersResponse result = orderService.generateOrdersFromPlan("plan-1", null, "user-1");

            assertThat(result.getOrdersGenerated()).isEqualTo(1);
            verify(planRepository).save(any()); // Plan status updated
        }

        @Test
        @DisplayName("generateOrders updates plan status to IN_PRODUCTION")
        void generateOrders_updatesPlanStatus() {
            testPlan.setStatus(ProductionPlanStatus.APPROVED);

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));
            when(templateRepository.findActiveByIdWithStages(any()))
                    .thenReturn(Optional.of(testTemplate));
            when(orderRepository.save(any())).thenAnswer(i -> {
                ProductionOrder o = i.getArgument(0);
                if (o.getId() == null) o.setId("new-order");
                if (o.getStages() == null) o.setStages(new ArrayList<>());
                return o;
            });
            when(mapper.createStageFromTemplate(any(), any()))
                    .thenAnswer(i -> {
                        WorkflowStage ws = i.getArgument(0);
                        return ProductionOrderStage.builder()
                                .id("gen-stage-" + ws.getStageOrder())
                                .stageOrder(ws.getStageOrder())
                                .stageName(ws.getName())
                                .status(ws.getStageOrder() == 1
                                        ? ProductionOrderStageStatus.READY
                                        : ProductionOrderStageStatus.BLOCKED)
                                .isFinalStage(ws.getIsFinalStage())
                                .build();
                    });
            when(orderRepository.findLastOrderNumberWithPrefix(anyString()))
                    .thenReturn(Optional.empty());
            when(planRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toListResponseList(any())).thenReturn(List.of());

            orderService.generateOrdersFromPlan("plan-1", null, "user-1");

            assertThat(testPlan.getStatus()).isEqualTo(ProductionPlanStatus.IN_PRODUCTION);
            assertThat(testPlan.getActualStartDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("generateOrders from COMPLETED plan throws exception")
        void generateOrders_fromCompletedPlan_throwsException() {
            testPlan.setStatus(ProductionPlanStatus.COMPLETED);

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));

            assertThatThrownBy(() -> orderService.generateOrdersFromPlan("plan-1", null, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot generate orders");
        }

        @Test
        @DisplayName("generateOrders without template throws exception")
        void generateOrders_withoutTemplate_throwsException() {
            testPlan.setWorkflowTemplate(null);

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));

            assertThatThrownBy(() -> orderService.generateOrdersFromPlan("plan-1", null, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no workflow template");
        }

        @Test
        @DisplayName("generateOrders for specific JTRCs creates linked orders")
        void generateOrders_forJtrcIds_createsLinkedOrders() {
            testPlan.setStatus(ProductionPlanStatus.APPROVED);
            JewelryTechnicalReport jtrc1 = JewelryTechnicalReport.builder()
                    .id("jtrc-1").reportNumber("JTRC-001").build();
            JewelryTechnicalReport jtrc2 = JewelryTechnicalReport.builder()
                    .id("jtrc-2").reportNumber("JTRC-002").build();

            GenerateOrdersRequest request = GenerateOrdersRequest.builder()
                    .jtrcIds(List.of("jtrc-1", "jtrc-2"))
                    .build();

            when(planRepository.findActiveByIdWithWorkflowTemplate("plan-1"))
                    .thenReturn(Optional.of(testPlan));
            when(templateRepository.findActiveByIdWithStages(any()))
                    .thenReturn(Optional.of(testTemplate));
            when(jtrcRepository.findActiveByIdWithComponents("jtrc-1")).thenReturn(Optional.of(jtrc1));
            when(jtrcRepository.findActiveByIdWithComponents("jtrc-2")).thenReturn(Optional.of(jtrc2));
            when(orderRepository.save(any())).thenAnswer(i -> {
                ProductionOrder o = i.getArgument(0);
                if (o.getId() == null) o.setId("order-" + System.nanoTime());
                if (o.getStages() == null) o.setStages(new ArrayList<>());
                return o;
            });
            when(mapper.createStageFromTemplate(any(), any()))
                    .thenAnswer(i -> {
                        WorkflowStage ws = i.getArgument(0);
                        return ProductionOrderStage.builder()
                                .id("gen-stage-" + ws.getStageOrder() + "-" + System.nanoTime())
                                .stageOrder(ws.getStageOrder())
                                .stageName(ws.getName())
                                .status(ws.getStageOrder() == 1
                                        ? ProductionOrderStageStatus.READY
                                        : ProductionOrderStageStatus.BLOCKED)
                                .isFinalStage(ws.getIsFinalStage())
                                .build();
                    });
            when(orderRepository.findLastOrderNumberWithPrefix(anyString()))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of("PO-2026-02-0001"));
            when(planRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toListResponseList(any())).thenReturn(List.of());

            GenerateOrdersResponse result = orderService.generateOrdersFromPlan("plan-1", request, "user-1");

            assertThat(result.getOrdersGenerated()).isEqualTo(2);
        }
    }

    // ==================== Status Transition Tests ====================

    @Nested
    @DisplayName("Status Transition Validation")
    class StatusTransitions {

        @Test
        @DisplayName("DRAFT → READY is valid")
        void draftToReady_isValid() {
            testOrder.setStatus(ProductionOrderStatus.DRAFT);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.updateStatus("order-1",
                    ProductionOrderStatus.READY, "user-1", null);

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStatus.READY);
        }

        @Test
        @DisplayName("DRAFT → CANCELLED is valid")
        void draftToCancelled_isValid() {
            testOrder.setStatus(ProductionOrderStatus.DRAFT);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.updateStatus("order-1",
                    ProductionOrderStatus.CANCELLED, "user-1", null);

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("READY → IN_PROGRESS is valid")
        void readyToInProgress_isValid() {
            testOrder.setStatus(ProductionOrderStatus.READY);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.updateStatus("order-1",
                    ProductionOrderStatus.IN_PROGRESS, "user-1", null);

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED is valid")
        void inProgressToCompleted_isValid() {
            testOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProductionOrder result = orderService.updateStatus("order-1",
                    ProductionOrderStatus.COMPLETED, "user-1", null);

            assertThat(result.getStatus()).isEqualTo(ProductionOrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("COMPLETED → any state is invalid")
        void completedToAny_isInvalid() {
            testOrder.setStatus(ProductionOrderStatus.COMPLETED);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateStatus("order-1",
                    ProductionOrderStatus.IN_PROGRESS, "user-1", null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CANCELLED → any state is invalid")
        void cancelledToAny_isInvalid() {
            testOrder.setStatus(ProductionOrderStatus.CANCELLED);

            when(orderRepository.findActiveById("order-1")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateStatus("order-1",
                    ProductionOrderStatus.READY, "user-1", null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ==================== Counting Tests ====================

    @Nested
    @DisplayName("Counting Operations")
    class CountingOperations {

        @Test
        @DisplayName("countActive returns correct count")
        void countActive_returnsCorrectCount() {
            when(orderRepository.countActive()).thenReturn(42L);

            long result = orderService.countActive();

            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("countByProductionPlanId returns correct count")
        void countByPlanId_returnsCorrectCount() {
            when(orderRepository.countByProductionPlanId("plan-1")).thenReturn(5);

            int result = orderService.countByProductionPlanId("plan-1");

            assertThat(result).isEqualTo(5);
        }
    }
}
