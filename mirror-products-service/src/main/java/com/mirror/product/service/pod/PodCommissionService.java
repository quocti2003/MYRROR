package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.pod.Pod;
import com.mirror.product.entity.pod.PodAttribution;
import com.mirror.product.entity.pod.PodCommission;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.enums.AttributionStatus;
import com.mirror.product.enums.CommissionStatus;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.exception.pod.PartnerNotFoundException;
import com.mirror.product.repository.pod.PodAttributionRepository;
import com.mirror.product.repository.pod.PodCommissionRepository;
import com.mirror.product.repository.pod.PodPartnerRepository;
import com.mirror.product.repository.pod.PodRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PodCommissionService {

    private final PodCommissionRepository commissionRepository;
    private final PodAttributionRepository attributionRepository;
    private final PodPartnerRepository partnerRepository;
    private final PodRepository podRepository;

    /**
     * Generate commission for a partner for a specific period.
     */
    @Transactional
    public CommissionResponse generateCommission(CommissionGenerateRequest request) {
        log.info("Generating commission for partner: {} for period {} to {}",
                request.getPartnerId(), request.getPeriodStart(), request.getPeriodEnd());

        // Validate partner
        PodPartner partner = partnerRepository.findActiveById(request.getPartnerId())
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + request.getPartnerId()));

        // Check if commission already exists for this period
        Optional<PodCommission> existing = commissionRepository.findByPartnerIdAndPeriod(
                request.getPartnerId(), request.getPeriodStart(), request.getPeriodEnd());

        if (existing.isPresent() && !request.isRegenerate()) {
            log.info("Commission already exists for partner {} and period, returning existing",
                    request.getPartnerId());
            return CommissionResponse.fromEntity(existing.get());
        }

        // If regenerating, delete the existing commission
        if (existing.isPresent() && request.isRegenerate()) {
            PodCommission old = existing.get();
            if (old.getStatus() == CommissionStatus.PAID) {
                throw new IllegalStateException("Cannot regenerate a paid commission");
            }
            // Unlink attributions from old commission
            unlinkAttributionsFromCommission(old.getId());
            old.setIsDeleted(true);
            commissionRepository.save(old);
        }

        // Get confirmed attributions for this period
        Instant periodStart = request.getPeriodStart().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant periodEnd = request.getPeriodEnd().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<PodAttribution> attributions = attributionRepository.findByPartnerIdAndDateRange(
                request.getPartnerId(), periodStart, periodEnd);

        // Filter only confirmed attributions without commission
        List<PodAttribution> eligibleAttributions = attributions.stream()
                .filter(a -> a.getStatus() == AttributionStatus.CONFIRMED)
                .filter(a -> a.getCommissionId() == null)
                .toList();

        if (eligibleAttributions.isEmpty()) {
            log.info("No eligible attributions found for partner {} in period", request.getPartnerId());
            // Still create a zero commission record
        }

        // Calculate totals
        int totalOrders = eligibleAttributions.size();
        BigDecimal totalOrderAmount = eligibleAttributions.stream()
                .map(PodAttribution::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal attributedAmount = eligibleAttributions.stream()
                .map(PodAttribution::getAttributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate commission using POD-specific rate when available, otherwise use partner rate
        BigDecimal partnerDefaultRate = partner.getCommissionRate();
        BigDecimal commissionAmount = BigDecimal.ZERO;

        for (PodAttribution attribution : eligibleAttributions) {
            BigDecimal rate = partnerDefaultRate;

            // Check if POD has a specific commission rate
            if (attribution.getPodId() != null) {
                Pod pod = podRepository.findById(attribution.getPodId()).orElse(null);
                if (pod != null && pod.getCommissionRate() != null) {
                    rate = pod.getCommissionRate();
                }
            }

            // Calculate commission for this attribution
            BigDecimal attributionCommission = attribution.getAttributedAmount()
                    .multiply(rate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            commissionAmount = commissionAmount.add(attributionCommission);
        }

        // Use the partner's default rate for the commission record (for display purposes)
        // The actual amount reflects POD-specific rates
        BigDecimal commissionRate = partnerDefaultRate;

        // Create commission
        PodCommission commission = PodCommission.builder()
                .partnerId(request.getPartnerId())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .totalOrders(totalOrders)
                .totalOrderAmount(totalOrderAmount)
                .attributedAmount(attributedAmount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .finalAmount(commissionAmount)
                .status(CommissionStatus.PENDING)
                .build();

        commission = commissionRepository.save(commission);

        // Link attributions to this commission
        final String commissionId = commission.getId();
        for (PodAttribution attribution : eligibleAttributions) {
            attribution.setCommissionId(commissionId);
        }
        attributionRepository.saveAll(eligibleAttributions);

        log.info("Commission generated: {} with {} orders, amount: {}",
                commission.getId(), totalOrders, commissionAmount);

        return CommissionResponse.fromEntity(commission);
    }

    /**
     * Generate commissions for multiple partners in batch.
     */
    @Transactional
    public List<CommissionResponse> generateCommissionsBatch(CommissionBatchGenerateRequest request) {
        log.info("Batch generating commissions for period {} to {}",
                request.getPeriodStart(), request.getPeriodEnd());

        List<String> partnerIds;
        if (request.getPartnerIds() != null && !request.getPartnerIds().isEmpty()) {
            partnerIds = new ArrayList<>(request.getPartnerIds());
        } else {
            // Get all active partners
            partnerIds = partnerRepository.findByStatusAndIsDeletedFalse(PartnerStatus.ACTIVE).stream()
                    .map(PodPartner::getId)
                    .toList();
        }

        List<CommissionResponse> results = new ArrayList<>();
        for (String partnerId : partnerIds) {
            try {
                CommissionGenerateRequest genRequest = CommissionGenerateRequest.builder()
                        .partnerId(partnerId)
                        .periodStart(request.getPeriodStart())
                        .periodEnd(request.getPeriodEnd())
                        .regenerate(request.isRegenerate())
                        .build();
                results.add(generateCommission(genRequest));
            } catch (Exception e) {
                log.error("Failed to generate commission for partner: {}", partnerId, e);
            }
        }

        log.info("Batch commission generation completed: {} commissions created", results.size());
        return results;
    }

    /**
     * Get commission by ID.
     */
    @Transactional(readOnly = true)
    public CommissionResponse getCommission(String commissionId) {
        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));
        return CommissionResponse.fromEntity(commission);
    }

    /**
     * Get commission detail with attributions.
     */
    @Transactional(readOnly = true)
    public CommissionDetailResponse getCommissionDetail(String commissionId) {
        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        CommissionDetailResponse response = CommissionDetailResponse.fromEntity(commission);

        // Get linked attributions
        List<PodAttribution> attributions = attributionRepository.findByCommissionId(commissionId);
        List<CommissionDetailResponse.AttributionSummary> summaries = attributions.stream()
                .map(a -> {
                    var builder = CommissionDetailResponse.AttributionSummary.builder()
                            .id(a.getId())
                            .orderId(a.getOrderId())
                            .podName(a.getPod() != null ? a.getPod().getName() : null)
                            .orderAmount(a.getOrderAmount())
                            .attributedAmount(a.getAttributedAmount())
                            .orderPlacedAt(a.getOrderPlacedAt())
                            .daysToConversion(a.getDaysToConversion());

                    if (a.getOrder() != null) {
                        builder.customerName(a.getOrder().getCustomerName())
                                .customerEmail(a.getOrder().getCustomerEmail())
                                .customerPhone(a.getOrder().getCustomerPhone());
                    }

                    return builder.build();
                })
                .toList();

        return response.withAttributions(summaries);
    }

    /**
     * Get commissions by partner.
     */
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getCommissionsByPartner(String partnerId, Pageable pageable) {
        return commissionRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(CommissionResponse::fromEntity);
    }

    /**
     * Search commissions with criteria.
     */
    @Transactional(readOnly = true)
    public Page<CommissionResponse> searchCommissions(CommissionSearchCriteria criteria, Pageable pageable) {
        Specification<PodCommission> spec = buildSearchSpecification(criteria);
        return commissionRepository.findAll(spec, pageable).map(CommissionResponse::fromEntity);
    }

    /**
     * Add adjustment to a commission.
     */
    @Transactional
    public CommissionResponse addAdjustment(String commissionId, CommissionAdjustRequest request) {
        log.info("Adding adjustment to commission: {}, amount: {}", commissionId, request.getAmount());

        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        if (commission.getStatus() == CommissionStatus.PAID) {
            throw new IllegalStateException("Cannot adjust a paid commission");
        }

        BigDecimal currentAdjustments = commission.getAdjustments() != null
                ? commission.getAdjustments()
                : BigDecimal.ZERO;
        commission.setAdjustments(currentAdjustments.add(request.getAmount()));

        String existingReason = commission.getAdjustmentReason() != null
                ? commission.getAdjustmentReason() + "; "
                : "";
        commission.setAdjustmentReason(existingReason + request.getReason());

        commission.calculateFinalAmount();
        commission = commissionRepository.save(commission);

        log.info("Adjustment added to commission: {}", commissionId);
        return CommissionResponse.fromEntity(commission);
    }

    /**
     * Approve a commission.
     */
    @Transactional
    public CommissionResponse approveCommission(String commissionId, String approver) {
        log.info("Approving commission: {} by {}", commissionId, approver);

        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        if (commission.getStatus() != CommissionStatus.PENDING) {
            throw new IllegalStateException("Only pending commissions can be approved");
        }

        commission.approve(approver);
        commission = commissionRepository.save(commission);

        log.info("Commission approved: {}", commissionId);
        return CommissionResponse.fromEntity(commission);
    }

    /**
     * Mark commission as paid.
     */
    @Transactional
    public CommissionResponse markAsPaid(String commissionId, CommissionPaymentRequest request) {
        log.info("Marking commission as paid: {}", commissionId);

        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        if (commission.getStatus() != CommissionStatus.APPROVED) {
            throw new IllegalStateException("Only approved commissions can be marked as paid");
        }

        commission.markAsPaid(request.getPaymentReference(), request.getPaymentMethod());
        if (request.getNotes() != null) {
            String existingNotes = commission.getNotes() != null
                    ? commission.getNotes() + "; "
                    : "";
            commission.setNotes(existingNotes + request.getNotes());
        }
        commission = commissionRepository.save(commission);

        log.info("Commission marked as paid: {}", commissionId);
        return CommissionResponse.fromEntity(commission);
    }

    /**
     * Cancel a commission.
     */
    @Transactional
    public CommissionResponse cancelCommission(String commissionId, String reason) {
        log.info("Cancelling commission: {}", commissionId);

        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        if (commission.getStatus() == CommissionStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid commission");
        }

        // Unlink attributions
        unlinkAttributionsFromCommission(commissionId);

        commission.setStatus(CommissionStatus.CANCELLED);
        commission.setNotes(reason);
        commission = commissionRepository.save(commission);

        log.info("Commission cancelled: {}", commissionId);
        return CommissionResponse.fromEntity(commission);
    }

    /**
     * Delete commission (soft delete).
     */
    @Transactional
    public void deleteCommission(String commissionId) {
        log.info("Deleting commission: {}", commissionId);

        PodCommission commission = commissionRepository.findById(commissionId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        if (commission.getStatus() == CommissionStatus.PAID) {
            throw new IllegalStateException("Cannot delete a paid commission");
        }

        // Unlink attributions
        unlinkAttributionsFromCommission(commissionId);

        commission.setIsDeleted(true);
        commissionRepository.save(commission);

        log.info("Commission deleted: {}", commissionId);
    }

    /**
     * Get commission summary statistics.
     */
    @Transactional(readOnly = true)
    public CommissionSummary getCommissionSummary() {
        long pendingCount = commissionRepository.countByStatus(CommissionStatus.PENDING);
        long approvedCount = commissionRepository.countByStatus(CommissionStatus.APPROVED);
        long paidCount = commissionRepository.countByStatus(CommissionStatus.PAID);
        long cancelledCount = commissionRepository.countByStatus(CommissionStatus.CANCELLED);

        BigDecimal totalPending = commissionRepository.sumTotalPendingAmount();

        return CommissionSummary.builder()
                .totalCommissions(pendingCount + approvedCount + paidCount + cancelledCount)
                .pendingCommissions(pendingCount)
                .approvedCommissions(approvedCount)
                .paidCommissions(paidCount)
                .cancelledCommissions(cancelledCount)
                .totalPendingAmount(totalPending)
                .build();
    }

    /**
     * Get commission summary for a specific partner.
     */
    @Transactional(readOnly = true)
    public CommissionSummary getPartnerCommissionSummary(String partnerId) {
        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));

        BigDecimal paidAmount = commissionRepository.sumPaidAmountByPartnerId(partnerId);
        BigDecimal pendingAmount = commissionRepository.sumPendingAmountByPartnerId(partnerId);
        long totalCommissions = commissionRepository.countByPartnerId(partnerId);

        return CommissionSummary.builder()
                .partnerId(partnerId)
                .partnerName(partner.getBusinessName())
                .partnerTotalEarned(paidAmount)
                .partnerPendingAmount(pendingAmount)
                .totalCommissions(totalCommissions)
                .build();
    }

    /**
     * Generate monthly commission for last month (scheduled job).
     */
    @Transactional
    public List<CommissionResponse> generateMonthlyCommissions() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate periodEnd = today.withDayOfMonth(1).minusDays(1);

        log.info("Generating monthly commissions for period: {} to {}", periodStart, periodEnd);

        CommissionBatchGenerateRequest request = CommissionBatchGenerateRequest.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .regenerate(false)
                .build();

        return generateCommissionsBatch(request);
    }

    // ========== Private Helper Methods ==========

    private void unlinkAttributionsFromCommission(String commissionId) {
        List<PodAttribution> attributions = attributionRepository.findByCommissionId(commissionId);
        for (PodAttribution attribution : attributions) {
            attribution.setCommissionId(null);
        }
        attributionRepository.saveAll(attributions);
    }

    private Specification<PodCommission> buildSearchSpecification(CommissionSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (criteria.getPartnerId() != null && !criteria.getPartnerId().isBlank()) {
                predicates.add(cb.equal(root.get("partnerId"), criteria.getPartnerId()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getPeriodStartAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("periodStart"), criteria.getPeriodStartAfter()));
            }

            if (criteria.getPeriodStartBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("periodStart"), criteria.getPeriodStartBefore()));
            }

            if (criteria.getPeriodEndAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("periodEnd"), criteria.getPeriodEndAfter()));
            }

            if (criteria.getPeriodEndBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("periodEnd"), criteria.getPeriodEndBefore()));
            }

            if (criteria.getMinFinalAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("finalAmount"), criteria.getMinFinalAmount()));
            }

            if (criteria.getMaxFinalAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("finalAmount"), criteria.getMaxFinalAmount()));
            }

            if (criteria.getIsPaid() != null) {
                if (criteria.getIsPaid()) {
                    predicates.add(cb.equal(root.get("status"), CommissionStatus.PAID));
                } else {
                    predicates.add(cb.notEqual(root.get("status"), CommissionStatus.PAID));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
