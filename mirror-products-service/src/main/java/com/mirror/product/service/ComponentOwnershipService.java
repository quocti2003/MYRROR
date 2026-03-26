package com.mirror.product.service;

import com.mirror.product.dto.componentownership.*;
import com.mirror.product.entity.*;
import com.mirror.product.enums.HandoffStatus;
import com.mirror.product.enums.HandoffType;
import com.mirror.product.mapper.ComponentOwnershipLogMapper;
import com.mirror.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing Component Ownership tracking.
 *
 * Provides an immutable audit trail of component ownership transfers throughout
 * the production workflow. Every time a component changes hands (vendor to vendor,
 * vendor to MIRROR, etc.), a new log entry is created.
 *
 * This enables:
 * - Full traceability of component location history
 * - Audit compliance for production chain
 * - Dispute resolution (who had what, when)
 * - Transit time analysis
 * - Vendor performance metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentOwnershipService {

    private final ComponentOwnershipLogRepository ownershipLogRepository;
    private final ProductionOrderRepository orderRepository;
    private final ProductionOrderStageRepository stageRepository;
    private final VendorRepository vendorRepository;
    private final ComponentOwnershipLogMapper mapper;

    // ==================== Query Operations ====================

    /**
     * Get ownership log by ID
     */
    @Transactional(readOnly = true)
    public Optional<ComponentOwnershipLogDTO> findById(String id) {
        return ownershipLogRepository.findById(id)
                .filter(log -> !log.getIsDeleted())
                .map(mapper::toDTO);
    }

    /**
     * Get full ownership history for a production order
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getOrderHistory(String orderId) {
        return ownershipLogRepository.findByProductionOrderId(orderId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get ownership history for a production order with pagination
     */
    @Transactional(readOnly = true)
    public Page<ComponentOwnershipLogDTO> getOrderHistory(String orderId, Pageable pageable) {
        return ownershipLogRepository.findByProductionOrderId(orderId, pageable)
                .map(mapper::toDTO);
    }

    /**
     * Get pending receipts for a vendor (items they need to confirm receiving)
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getPendingReceipts(String vendorId) {
        return ownershipLogRepository.findPendingByToVendorId(vendorId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get pending receipts for a vendor with pagination
     */
    @Transactional(readOnly = true)
    public Page<ComponentOwnershipLogDTO> getPendingReceipts(String vendorId, Pageable pageable) {
        return ownershipLogRepository.findPendingByToVendorId(vendorId, pageable)
                .map(mapper::toDTO);
    }

    /**
     * Get overdue handoffs
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getOverdueHandoffs() {
        return ownershipLogRepository.findOverdueHandoffs()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get overdue handoffs for a specific vendor
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getOverdueHandoffsForVendor(String vendorId) {
        return ownershipLogRepository.findOverdueByToVendorId(vendorId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get handoffs by status
     */
    @Transactional(readOnly = true)
    public Page<ComponentOwnershipLogDTO> findByStatus(HandoffStatus status, Pageable pageable) {
        return ownershipLogRepository.findByStatus(status, pageable)
                .map(mapper::toDTO);
    }

    /**
     * Get items currently held by a vendor
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getItemsHeldByVendor(String vendorId) {
        return ownershipLogRepository.findCurrentlyHeldByVendor(vendorId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get handoffs sent by a vendor
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getHandoffsSentByVendor(String vendorId) {
        return ownershipLogRepository.findByFromVendorId(vendorId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get handoffs received by a vendor
     */
    @Transactional(readOnly = true)
    public List<ComponentOwnershipLogDTO> getHandoffsReceivedByVendor(String vendorId) {
        return ownershipLogRepository.findByToVendorId(vendorId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get the latest handoff for an order
     */
    @Transactional(readOnly = true)
    public Optional<ComponentOwnershipLogDTO> getLatestHandoff(String orderId) {
        return ownershipLogRepository.findLatestByOrderId(orderId)
                .map(mapper::toDTO);
    }

    /**
     * Check if order has an active (non-terminal) handoff
     */
    @Transactional(readOnly = true)
    public boolean hasActiveHandoff(String orderId) {
        return ownershipLogRepository.hasActiveHandoff(orderId);
    }

    /**
     * Count pending handoffs for a vendor
     */
    @Transactional(readOnly = true)
    public long countPendingForVendor(String vendorId) {
        return ownershipLogRepository.countPendingByToVendorId(vendorId);
    }

    // ==================== Handoff Operations ====================

    /**
     * Initiate a new handoff
     */
    @Transactional
    public ComponentOwnershipLogDTO initiateHandoff(InitiateHandoffRequest request, String userId) {
        log.info("Initiating handoff for order: {} by user: {}", request.getProductionOrderId(), userId);

        // Validate production order
        ProductionOrder order = orderRepository.findActiveById(request.getProductionOrderId())
                .orElseThrow(() -> new RuntimeException("Production order not found: " + request.getProductionOrderId()));

        // Check for existing active handoff
        if (ownershipLogRepository.hasActiveHandoff(order.getId())) {
            throw new IllegalStateException("Order already has an active handoff in progress");
        }

        // Create handoff log
        ComponentOwnershipLog handoffLog = ComponentOwnershipLog.builder()
                .productionOrder(order)
                .handoffType(request.getHandoffType())
                .status(HandoffStatus.INITIATED)
                .initiatedBy(userId)
                .initiatedAt(LocalDateTime.now())
                .expectedArrivalDate(request.getExpectedArrivalDate())
                .reason(request.getReason())
                .notes(request.getNotes())
                .build();

        // Set stage if provided
        if (request.getStageId() != null) {
            ProductionOrderStage stage = stageRepository.findById(request.getStageId())
                    .orElseThrow(() -> new RuntimeException("Stage not found: " + request.getStageId()));
            handoffLog.setStage(stage);
        }

        // Set from vendor if provided
        if (request.getFromVendorId() != null) {
            Vendor fromVendor = vendorRepository.findById(request.getFromVendorId())
                    .orElseThrow(() -> new RuntimeException("From vendor not found: " + request.getFromVendorId()));
            handoffLog.setFromVendor(fromVendor);
        }

        // Set to vendor if provided
        if (request.getToVendorId() != null) {
            Vendor toVendor = vendorRepository.findById(request.getToVendorId())
                    .orElseThrow(() -> new RuntimeException("To vendor not found: " + request.getToVendorId()));
            handoffLog.setToVendor(toVendor);
        }

        handoffLog = ownershipLogRepository.save(handoffLog);
        log.info("Created handoff log: {} for order: {}", handoffLog.getId(), order.getId());

        return mapper.toDTO(handoffLog);
    }

    /**
     * Mark handoff as in-transit
     */
    @Transactional
    public ComponentOwnershipLogDTO markInTransit(String handoffId, MarkInTransitRequest request, String userId) {
        log.info("Marking handoff {} as in-transit by user: {}", handoffId, userId);

        ComponentOwnershipLog handoffLog = ownershipLogRepository.findById(handoffId)
                .orElseThrow(() -> new RuntimeException("Handoff not found: " + handoffId));

        if (handoffLog.getStatus() != HandoffStatus.INITIATED) {
            throw new IllegalStateException("Can only mark INITIATED handoffs as in-transit");
        }

        handoffLog.markInTransit(request.getTrackingNumber(), request.getShippingCarrier());

        if (request.getExpectedArrivalDate() != null) {
            handoffLog.setExpectedArrivalDate(request.getExpectedArrivalDate());
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = handoffLog.getNotes() != null ? handoffLog.getNotes() + "\n" : "";
            handoffLog.setNotes(existingNotes + "[In Transit] " + request.getNotes());
        }

        handoffLog = ownershipLogRepository.save(handoffLog);
        log.info("Marked handoff {} as in-transit", handoffId);

        return mapper.toDTO(handoffLog);
    }

    /**
     * Confirm receipt of a handoff
     */
    @Transactional
    public ComponentOwnershipLogDTO confirmReceipt(String handoffId, ConfirmReceiptRequest request, String userId) {
        log.info("Confirming receipt for handoff {} by user: {}", handoffId, userId);

        ComponentOwnershipLog handoffLog = ownershipLogRepository.findById(handoffId)
                .orElseThrow(() -> new RuntimeException("Handoff not found: " + handoffId));

        if (!handoffLog.canConfirmReceipt()) {
            throw new IllegalStateException("Cannot confirm receipt for handoff in status: " + handoffLog.getStatus());
        }

        handoffLog.confirmReceipt(userId);

        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = handoffLog.getNotes() != null ? handoffLog.getNotes() + "\n" : "";
            handoffLog.setNotes(existingNotes + "[Receipt] " + request.getNotes());
        }

        handoffLog = ownershipLogRepository.save(handoffLog);

        // Update production order's current holder
        ProductionOrder order = handoffLog.getProductionOrder();
        if (handoffLog.getToVendor() != null) {
            order.setCurrentHolderId(handoffLog.getToVendor().getId());
        } else {
            // Returned to MIRROR
            order.setCurrentHolderId(null);
        }
        orderRepository.save(order);

        log.info("Confirmed receipt for handoff {} - component now with: {}",
                handoffId, handoffLog.getToVendor() != null ? handoffLog.getToVendor().getId() : "MIRROR");

        return mapper.toDTO(handoffLog);
    }

    /**
     * Reject a handoff
     */
    @Transactional
    public ComponentOwnershipLogDTO rejectHandoff(String handoffId, RejectHandoffRequest request, String userId) {
        log.info("Rejecting handoff {} by user: {}", handoffId, userId);

        ComponentOwnershipLog handoffLog = ownershipLogRepository.findById(handoffId)
                .orElseThrow(() -> new RuntimeException("Handoff not found: " + handoffId));

        if (!handoffLog.canReject()) {
            throw new IllegalStateException("Cannot reject handoff in status: " + handoffLog.getStatus());
        }

        handoffLog.reject(userId, request.getRejectionReason());

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = handoffLog.getNotes() != null ? handoffLog.getNotes() + "\n" : "";
            handoffLog.setNotes(existingNotes + "[Rejection] " + request.getNotes());
        }

        handoffLog = ownershipLogRepository.save(handoffLog);
        log.info("Rejected handoff {}: {}", handoffId, request.getRejectionReason());

        return mapper.toDTO(handoffLog);
    }

    /**
     * Cancel a handoff (only INITIATED status)
     */
    @Transactional
    public ComponentOwnershipLogDTO cancelHandoff(String handoffId, String reason, String userId) {
        log.info("Cancelling handoff {} by user: {}", handoffId, userId);

        ComponentOwnershipLog handoffLog = ownershipLogRepository.findById(handoffId)
                .orElseThrow(() -> new RuntimeException("Handoff not found: " + handoffId));

        if (!handoffLog.canCancel()) {
            throw new IllegalStateException("Can only cancel INITIATED handoffs");
        }

        handoffLog.setStatus(HandoffStatus.CANCELLED);
        handoffLog.setReceivedBy(userId);
        handoffLog.setReceivedAt(LocalDateTime.now());

        if (reason != null && !reason.isBlank()) {
            String existingNotes = handoffLog.getNotes() != null ? handoffLog.getNotes() + "\n" : "";
            handoffLog.setNotes(existingNotes + "[Cancelled] " + reason);
        }

        handoffLog = ownershipLogRepository.save(handoffLog);
        log.info("Cancelled handoff {}", handoffId);

        return mapper.toDTO(handoffLog);
    }

    // ==================== Stage Lifecycle Integration ====================

    /**
     * Create handoff log when a stage is started (component goes to assigned vendor)
     */
    @Transactional
    public ComponentOwnershipLogDTO createStageStartHandoff(ProductionOrderStage stage, String userId) {
        if (stage.getAssignedVendor() == null) {
            throw new IllegalStateException("Cannot create handoff for stage without assigned vendor");
        }

        // Find current holder
        String currentHolderId = stage.getProductionOrder().getCurrentHolderId();
        Vendor fromVendor = currentHolderId != null ?
                vendorRepository.findById(currentHolderId).orElse(null) : null;

        InitiateHandoffRequest request = InitiateHandoffRequest.builder()
                .productionOrderId(stage.getProductionOrder().getId())
                .stageId(stage.getId())
                .fromVendorId(fromVendor != null ? fromVendor.getId() : null)
                .toVendorId(stage.getAssignedVendor().getId())
                .handoffType(HandoffType.STAGE_START)
                .reason("Stage started: " + stage.getStageName())
                .build();

        return initiateHandoff(request, userId);
    }

    /**
     * Create handoff log when a stage is completed (component goes to next vendor or back to MIRROR)
     */
    @Transactional
    public ComponentOwnershipLogDTO createStageCompleteHandoff(ProductionOrderStage stage,
                                                               Vendor nextVendor,
                                                               String userId) {
        InitiateHandoffRequest request = InitiateHandoffRequest.builder()
                .productionOrderId(stage.getProductionOrder().getId())
                .stageId(stage.getId())
                .fromVendorId(stage.getAssignedVendor() != null ? stage.getAssignedVendor().getId() : null)
                .toVendorId(nextVendor != null ? nextVendor.getId() : null)
                .handoffType(nextVendor != null ? HandoffType.STAGE_COMPLETE : HandoffType.RETURN_TO_MIRROR)
                .reason("Stage completed: " + stage.getStageName())
                .build();

        return initiateHandoff(request, userId);
    }

    /**
     * Create initial assignment handoff (MIRROR sends to first vendor)
     */
    @Transactional
    public ComponentOwnershipLogDTO createInitialAssignment(ProductionOrder order, Vendor toVendor, String userId) {
        InitiateHandoffRequest request = InitiateHandoffRequest.builder()
                .productionOrderId(order.getId())
                .fromVendorId(null) // From MIRROR
                .toVendorId(toVendor.getId())
                .handoffType(HandoffType.INITIAL_ASSIGNMENT)
                .reason("Initial assignment from MIRROR")
                .build();

        return initiateHandoff(request, userId);
    }

    /**
     * Create vendor reassignment handoff
     */
    @Transactional
    public ComponentOwnershipLogDTO createVendorReassignment(ProductionOrder order,
                                                              Vendor fromVendor,
                                                              Vendor toVendor,
                                                              String reason,
                                                              String userId) {
        InitiateHandoffRequest request = InitiateHandoffRequest.builder()
                .productionOrderId(order.getId())
                .fromVendorId(fromVendor != null ? fromVendor.getId() : null)
                .toVendorId(toVendor.getId())
                .handoffType(HandoffType.VENDOR_REASSIGN)
                .reason(reason)
                .build();

        return initiateHandoff(request, userId);
    }
}
