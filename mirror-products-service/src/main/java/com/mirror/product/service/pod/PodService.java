package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.Location;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.pod.Pod;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.entity.pod.PodQrCode;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PodStatus;
import com.mirror.product.enums.QrCodeStatus;
import com.mirror.product.exception.pod.PartnerNotFoundException;
import com.mirror.product.exception.pod.PodNotFoundException;
import com.mirror.product.repository.LocationRepository;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.pod.*;
import com.mirror.product.service.BaseService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PodService extends BaseService<Pod, String> {

    private final PodRepository podRepository;
    private final PodPartnerRepository partnerRepository;
    private final PodQrCodeRepository qrCodeRepository;
    private final PodQrScanRepository scanRepository;
    private final PodAttributionRepository attributionRepository;
    private final MirrorProductRepository productRepository;
    private final LocationRepository locationRepository;

    public PodService(
            PodRepository podRepository,
            PodPartnerRepository partnerRepository,
            PodQrCodeRepository qrCodeRepository,
            PodQrScanRepository scanRepository,
            PodAttributionRepository attributionRepository,
            MirrorProductRepository productRepository,
            LocationRepository locationRepository
    ) {
        super(podRepository);
        this.podRepository = podRepository;
        this.partnerRepository = partnerRepository;
        this.qrCodeRepository = qrCodeRepository;
        this.scanRepository = scanRepository;
        this.attributionRepository = attributionRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Create a new POD
     */
    @Transactional
    public PodResponse createPod(PodCreateRequest request) {
        log.info("Creating new POD: {} for partner: {}", request.getName(), request.getPartnerId());

        // Validate partner exists and is active
        PodPartner partner = partnerRepository.findActiveById(request.getPartnerId())
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + request.getPartnerId()));

        if (partner.getStatus() != PartnerStatus.ACTIVE && partner.getStatus() != PartnerStatus.APPROVED) {
            throw new IllegalStateException("Partner must be ACTIVE or APPROVED to create PODs");
        }

        // Create Location for map display
        Location location = createLocationForPod(request, partner);
        location = locationRepository.save(location);
        log.info("Location created for POD with ID: {}", location.getId());

        Pod pod = Pod.builder()
                .partnerId(request.getPartnerId())
                .name(request.getName())
                .description(request.getDescription())
                .locationName(request.getLocationName())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .displayCapacity(request.getDisplayCapacity() != null ? request.getDisplayCapacity() : 10)
                .commissionRate(request.getCommissionRate())
                .installationDate(request.getInstallationDate())
                .notes(request.getNotes())
                .status(PodStatus.DRAFT)
                .locationId(location.getId())
                .build();

        pod = podRepository.save(pod);

        // Assign products if provided
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            assignProductsToPod(pod.getId(), request.getProductIds());
            pod = podRepository.findById(pod.getId()).orElse(pod);
        }

        log.info("POD created successfully with ID: {}", pod.getId());
        return PodResponse.fromEntity(pod);
    }

    /**
     * Update an existing POD
     */
    @Transactional
    public PodResponse updatePod(String podId, PodUpdateRequest request) {
        log.info("Updating POD: {}", podId);

        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        if (request.getName() != null) {
            pod.setName(request.getName());
        }
        if (request.getDescription() != null) {
            pod.setDescription(request.getDescription());
        }
        if (request.getLocationName() != null) {
            pod.setLocationName(request.getLocationName());
        }
        if (request.getAddressLine1() != null) {
            pod.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            pod.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            pod.setCity(request.getCity());
        }
        if (request.getState() != null) {
            pod.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            pod.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            pod.setCountry(request.getCountry());
        }
        if (request.getLatitude() != null) {
            pod.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            pod.setLongitude(request.getLongitude());
        }
        if (request.getDisplayCapacity() != null) {
            pod.setDisplayCapacity(request.getDisplayCapacity());
        }
        // Always update commissionRate (can be null to use partner rate)
        pod.setCommissionRate(request.getCommissionRate());
        if (request.getInstallationDate() != null) {
            pod.setInstallationDate(request.getInstallationDate());
        }
        if (request.getLastMaintenanceDate() != null) {
            pod.setLastMaintenanceDate(request.getLastMaintenanceDate());
        }
        if (request.getNextMaintenanceDate() != null) {
            pod.setNextMaintenanceDate(request.getNextMaintenanceDate());
        }
        if (request.getNotes() != null) {
            pod.setNotes(request.getNotes());
        }

        // Update linked Location if exists
        updateLocationForPod(pod, request);

        pod = podRepository.save(pod);
        log.info("POD updated successfully: {}", podId);

        return PodResponse.fromEntity(pod);
    }

    /**
     * Update POD status
     */
    @Transactional
    public PodResponse updateStatus(String podId, PodStatusUpdateRequest request) {
        log.info("Updating POD status: {} to {}", podId, request.getStatus());

        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        PodStatus currentStatus = pod.getStatus();
        PodStatus newStatus = request.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        pod.setStatus(newStatus);

        // If deactivating, also deactivate all QR codes
        if (newStatus == PodStatus.INACTIVE) {
            qrCodeRepository.deactivateAllByPodId(podId);
            log.info("Deactivated all QR codes for POD: {}", podId);
        }

        pod = podRepository.save(pod);
        log.info("POD status updated: {} -> {}", currentStatus, newStatus);

        return PodResponse.fromEntity(pod);
    }

    /**
     * Assign products to a POD
     */
    @Transactional
    public PodResponse assignProducts(String podId, PodProductAssignRequest request) {
        log.info("Assigning {} products to POD: {}", request.getProductIds().size(), podId);

        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        if (request.isReplaceExisting()) {
            pod.getProducts().clear();
        }

        assignProductsToPod(podId, request.getProductIds());
        pod = podRepository.findById(podId).orElse(pod);

        log.info("Products assigned to POD: {}", podId);
        return PodResponse.fromEntity(pod);
    }

    /**
     * Remove a product from a POD
     */
    @Transactional
    public PodResponse removeProduct(String podId, String productId) {
        log.info("Removing product {} from POD: {}", productId, podId);

        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        pod.getProducts().removeIf(p -> p.getId().equals(productId));
        pod = podRepository.save(pod);

        // Deactivate QR code for this product if exists
        qrCodeRepository.findByPodIdAndProductId(podId, productId)
                .ifPresent(qr -> {
                    qr.setStatus(QrCodeStatus.INACTIVE);
                    qrCodeRepository.save(qr);
                });

        log.info("Product removed from POD: {}", podId);
        return PodResponse.fromEntity(pod);
    }

    /**
     * Get POD by ID
     */
    @Transactional(readOnly = true)
    public PodResponse getPod(String podId) {
        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));
        return PodResponse.fromEntity(pod);
    }

    /**
     * Get POD detail with statistics
     */
    @Transactional(readOnly = true)
    public PodDetailResponse getPodDetail(String podId) {
        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        PodDetailResponse response = PodDetailResponse.fromEntity(pod);

        // Get product info with QR codes
        List<PodDetailResponse.PodProductInfo> productInfos = new ArrayList<>();
        for (MirrorProduct product : pod.getProducts()) {
            PodQrCode qrCode = qrCodeRepository.findByPodIdAndProductId(podId, product.getId()).orElse(null);
            productInfos.add(PodDetailResponse.PodProductInfo.builder()
                    .productId(product.getId())
                    .productName(product.getItemName())
                    .productSku(product.getSkuCode())
                    .qrCodeId(qrCode != null ? qrCode.getId() : null)
                    .qrShortCode(qrCode != null ? qrCode.getShortCode() : null)
                    .scanCount(qrCode != null ? qrCode.getScanCount() : 0L)
                    .build());
        }
        response.withProducts(productInfos);

        // Calculate statistics
        long totalScans = scanRepository.countByPodId(podId);
        long totalAttributions = attributionRepository.countByPodId(podId);
        BigDecimal totalRevenue = attributionRepository.sumAttributedAmountByPartnerId(pod.getPartnerId());

        response.withStatistics(
                totalScans,
                totalScans, // For now, same as total - unique calculation would be more complex
                totalAttributions,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                totalScans > 0 ? (double) totalAttributions / totalScans * 100 : 0.0
        );

        return response;
    }

    /**
     * Search PODs with criteria
     */
    @Transactional(readOnly = true)
    public Page<PodResponse> searchPods(PodSearchCriteria criteria, Pageable pageable) {
        Specification<Pod> spec = buildSearchSpecification(criteria);
        return podRepository.findAll(spec, pageable).map(PodResponse::fromEntity);
    }

    /**
     * Get all PODs for a partner
     */
    @Transactional(readOnly = true)
    public Page<PodResponse> getPodsByPartner(String partnerId, Pageable pageable) {
        return podRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(PodResponse::fromEntity);
    }

    /**
     * Get all active PODs with pagination
     */
    @Transactional(readOnly = true)
    public Page<PodResponse> getAllPods(Pageable pageable) {
        return podRepository.findAllActive(pageable).map(PodResponse::fromEntity);
    }

    /**
     * Delete POD (soft delete)
     */
    @Transactional
    public void deletePod(String podId) {
        log.info("Deleting POD: {}", podId);

        Pod pod = findActiveById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        // Deactivate all QR codes first
        qrCodeRepository.deactivateAllByPodId(podId);

        softDeleteById(podId);
        log.info("POD deleted successfully: {}", podId);
    }

    /**
     * Get distinct cities
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCities() {
        return podRepository.findDistinctCities();
    }

    @Override
    public Pod update(String id, Pod entity) {
        Pod existing = findActiveById(id)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + id));

        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setLocationName(entity.getLocationName());
        existing.setAddressLine1(entity.getAddressLine1());
        existing.setAddressLine2(entity.getAddressLine2());
        existing.setCity(entity.getCity());
        existing.setState(entity.getState());
        existing.setPostalCode(entity.getPostalCode());
        existing.setCountry(entity.getCountry());
        existing.setLatitude(entity.getLatitude());
        existing.setLongitude(entity.getLongitude());
        existing.setDisplayCapacity(entity.getDisplayCapacity());
        existing.setInstallationDate(entity.getInstallationDate());
        existing.setLastMaintenanceDate(entity.getLastMaintenanceDate());
        existing.setNextMaintenanceDate(entity.getNextMaintenanceDate());
        existing.setNotes(entity.getNotes());

        return podRepository.save(existing);
    }

    // ========== Private Helper Methods ==========

    private void validateStatusTransition(PodStatus from, PodStatus to) {
        boolean valid = switch (from) {
            case DRAFT -> to == PodStatus.ACTIVE || to == PodStatus.INACTIVE;
            case ACTIVE -> to == PodStatus.MAINTENANCE || to == PodStatus.INACTIVE;
            case MAINTENANCE -> to == PodStatus.ACTIVE || to == PodStatus.INACTIVE;
            case INACTIVE -> to == PodStatus.ACTIVE || to == PodStatus.DRAFT;
        };

        if (!valid) {
            throw new IllegalStateException("Invalid status transition from " + from + " to " + to);
        }
    }

    private void assignProductsToPod(String podId, Set<String> productIds) {
        Pod pod = podRepository.findById(podId)
                .orElseThrow(() -> new PodNotFoundException("POD not found: " + podId));

        // Validate capacity
        if (productIds.size() > pod.getDisplayCapacity()) {
            throw new IllegalStateException(
                    "Cannot assign " + productIds.size() + " products. POD capacity is " + pod.getDisplayCapacity());
        }

        // Find all products
        List<MirrorProduct> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            Set<String> foundIds = products.stream().map(MirrorProduct::getId).collect(Collectors.toSet());
            Set<String> missingIds = new HashSet<>(productIds);
            missingIds.removeAll(foundIds);
            throw new IllegalArgumentException("Products not found: " + missingIds);
        }

        pod.getProducts().addAll(products);
        podRepository.save(pod);
    }

    private Specification<Pod> buildSearchSpecification(PodSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String keyword = "%" + criteria.getKeyword().toLowerCase() + "%";
                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("locationName")), keyword),
                        cb.like(cb.lower(root.get("city")), keyword)
                );
                predicates.add(keywordPredicate);
            }

            if (criteria.getPartnerId() != null && !criteria.getPartnerId().isBlank()) {
                predicates.add(cb.equal(root.get("partnerId"), criteria.getPartnerId()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getCity() != null && !criteria.getCity().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), criteria.getCity().toLowerCase()));
            }

            if (criteria.getCountry() != null && !criteria.getCountry().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("country")), criteria.getCountry().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create a Location entity for a POD (for map display).
     */
    private Location createLocationForPod(PodCreateRequest request, PodPartner partner) {
        // Build full address
        StringBuilder addressBuilder = new StringBuilder();
        if (request.getAddressLine1() != null) {
            addressBuilder.append(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null && !request.getAddressLine2().isBlank()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(request.getAddressLine2());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(request.getState());
        }
        if (request.getPostalCode() != null && !request.getPostalCode().isBlank()) {
            if (addressBuilder.length() > 0) addressBuilder.append(" ");
            addressBuilder.append(request.getPostalCode());
        }
        String address = addressBuilder.length() > 0 ? addressBuilder.toString() : "N/A";

        return Location.builder()
                .name(request.getName() + " - " + partner.getBusinessName())
                .type(Location.LocationType.POD)
                .address(address)
                .city(request.getCity() != null ? request.getCity() : "N/A")
                .latitude(request.getLatitude() != null ? request.getLatitude() : BigDecimal.ZERO)
                .longitude(request.getLongitude() != null ? request.getLongitude() : BigDecimal.ZERO)
                .hours("Contact partner for hours")
                .phone(partner.getContactPhone() != null ? partner.getContactPhone() : "N/A")
                .status(Location.LocationStatus.ACTIVE)
                .isInternal(false)
                .build();
    }

    /**
     * Update Location entity when POD location info changes.
     */
    private void updateLocationForPod(Pod pod, PodUpdateRequest request) {
        if (pod.getLocationId() == null) {
            return;
        }

        locationRepository.findById(pod.getLocationId()).ifPresent(location -> {
            boolean needsUpdate = false;

            // Update address if changed
            if (request.getAddressLine1() != null || request.getAddressLine2() != null ||
                request.getState() != null || request.getPostalCode() != null) {
                StringBuilder addressBuilder = new StringBuilder();
                String addr1 = request.getAddressLine1() != null ? request.getAddressLine1() : pod.getAddressLine1();
                String addr2 = request.getAddressLine2() != null ? request.getAddressLine2() : pod.getAddressLine2();
                String state = request.getState() != null ? request.getState() : pod.getState();
                String postal = request.getPostalCode() != null ? request.getPostalCode() : pod.getPostalCode();

                if (addr1 != null) addressBuilder.append(addr1);
                if (addr2 != null && !addr2.isBlank()) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(addr2);
                }
                if (state != null && !state.isBlank()) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(state);
                }
                if (postal != null && !postal.isBlank()) {
                    if (addressBuilder.length() > 0) addressBuilder.append(" ");
                    addressBuilder.append(postal);
                }
                if (addressBuilder.length() > 0) {
                    location.setAddress(addressBuilder.toString());
                    needsUpdate = true;
                }
            }

            // Update city if changed
            if (request.getCity() != null) {
                location.setCity(request.getCity());
                needsUpdate = true;
            }

            // Update coordinates if changed
            if (request.getLatitude() != null) {
                location.setLatitude(request.getLatitude());
                needsUpdate = true;
            }
            if (request.getLongitude() != null) {
                location.setLongitude(request.getLongitude());
                needsUpdate = true;
            }

            // Update name if POD name changed
            if (request.getName() != null && pod.getPartner() != null) {
                location.setName(request.getName() + " - " + pod.getPartner().getBusinessName());
                needsUpdate = true;
            }

            if (needsUpdate) {
                locationRepository.save(location);
                log.info("Location updated for POD: {}", pod.getId());
            }
        });
    }
}
