package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.entity.user.Role;
import com.mirror.product.entity.user.User;
import com.mirror.product.enums.PartnerStatus;
import com.mirror.product.enums.PartnerTier;
import com.mirror.product.enums.PodStatus;
import com.mirror.product.exception.pod.PartnerNotFoundException;
import com.mirror.product.exception.pod.PartnerAlreadyExistsException;
import com.mirror.product.exception.pod.InvalidPartnerStatusTransitionException;
import com.mirror.product.repository.pod.*;
import com.mirror.product.repository.user.RoleRepository;
import com.mirror.product.repository.user.UserRepository;
import com.mirror.product.service.BaseService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.mirror.product.enums.PartnerType;

@Service
@Slf4j
public class PodPartnerService extends BaseService<PodPartner, String> {

    private final PodPartnerRepository partnerRepository;
    private final PodRepository podRepository;
    private final PodQrScanRepository scanRepository;
    private final PodAttributionRepository attributionRepository;
    private final PodCommissionRepository commissionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public PodPartnerService(
            PodPartnerRepository partnerRepository,
            PodRepository podRepository,
            PodQrScanRepository scanRepository,
            PodAttributionRepository attributionRepository,
            PodCommissionRepository commissionRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        super(partnerRepository);
        this.partnerRepository = partnerRepository;
        this.podRepository = podRepository;
        this.scanRepository = scanRepository;
        this.attributionRepository = attributionRepository;
        this.commissionRepository = commissionRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new partner
     */
    @Transactional
    public PartnerResponse createPartner(PartnerCreateRequest request) {
        log.info("Creating new partner: {}", request.getBusinessName());

        // Check if email already exists
        if (partnerRepository.existsByContactEmail(request.getContactEmail())) {
            throw new PartnerAlreadyExistsException("Partner with email " + request.getContactEmail() + " already exists");
        }

        PodPartner partner = PodPartner.builder()
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .businessLicense(request.getBusinessLicense())
                .taxId(request.getTaxId())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .tier(request.getTier() != null ? request.getTier() : PartnerTier.BRONZE)
                .commissionRate(request.getCommissionRate() != null ? request.getCommissionRate() : new BigDecimal("5.00"))
                .partnerType(request.getPartnerType() != null ? request.getPartnerType() : PartnerType.LOCATION)
                .wholesaleDiscountRate(request.getWholesaleDiscountRate() != null ? request.getWholesaleDiscountRate()
                        : (PartnerType.PHYGITAL.equals(request.getPartnerType()) ? new BigDecimal("20.00") : null))
                .territory(request.getTerritory())
                .canSetOwnPrices(request.getCanSetOwnPrices() != null ? request.getCanSetOwnPrices() : false)
                .minMarkupPercent(request.getMinMarkupPercent())
                .maxMarkupPercent(request.getMaxMarkupPercent())
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .securityDeposit(request.getSecurityDeposit())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .bankBranch(request.getBankBranch())
                .notes(request.getNotes())
                .status(PartnerStatus.PENDING)
                .build();

        partner = partnerRepository.save(partner);
        log.info("Partner created successfully with ID: {}", partner.getId());

        return PartnerResponse.fromEntity(partner);
    }

    /**
     * Update an existing partner
     */
    @Transactional
    public PartnerResponse updatePartner(String partnerId, PartnerUpdateRequest request) {
        log.info("Updating partner: {}", partnerId);

        PodPartner partner = findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found with id: " + partnerId));

        // Check email uniqueness if changed
        if (request.getContactEmail() != null && !request.getContactEmail().equals(partner.getContactEmail())) {
            if (partnerRepository.existsByContactEmail(request.getContactEmail())) {
                throw new PartnerAlreadyExistsException("Partner with email " + request.getContactEmail() + " already exists");
            }
            partner.setContactEmail(request.getContactEmail());
        }

        if (request.getBusinessName() != null) {
            partner.setBusinessName(request.getBusinessName());
        }
        if (request.getBusinessType() != null) {
            partner.setBusinessType(request.getBusinessType());
        }
        if (request.getContactName() != null) {
            partner.setContactName(request.getContactName());
        }
        if (request.getContactPhone() != null) {
            partner.setContactPhone(request.getContactPhone());
        }
        if (request.getBusinessLicense() != null) {
            partner.setBusinessLicense(request.getBusinessLicense());
        }
        if (request.getTaxId() != null) {
            partner.setTaxId(request.getTaxId());
        }
        if (request.getAddressLine1() != null) {
            partner.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            partner.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            partner.setCity(request.getCity());
        }
        if (request.getState() != null) {
            partner.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            partner.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            partner.setCountry(request.getCountry());
        }
        if (request.getTier() != null) {
            partner.setTier(request.getTier());
        }
        if (request.getCommissionRate() != null) {
            partner.setCommissionRate(request.getCommissionRate());
        }
        if (request.getNotes() != null) {
            partner.setNotes(request.getNotes());
        }

        // PHYGITAL fields
        if (request.getPartnerType() != null) {
            partner.setPartnerType(request.getPartnerType());
        }
        if (request.getWholesaleDiscountRate() != null) {
            partner.setWholesaleDiscountRate(request.getWholesaleDiscountRate());
        }
        if (request.getTerritory() != null) {
            partner.setTerritory(request.getTerritory());
        }
        if (request.getCanSetOwnPrices() != null) {
            partner.setCanSetOwnPrices(request.getCanSetOwnPrices());
        }
        if (request.getMinMarkupPercent() != null) {
            partner.setMinMarkupPercent(request.getMinMarkupPercent());
        }
        if (request.getMaxMarkupPercent() != null) {
            partner.setMaxMarkupPercent(request.getMaxMarkupPercent());
        }
        if (request.getContractStartDate() != null) {
            partner.setContractStartDate(request.getContractStartDate());
        }
        if (request.getContractEndDate() != null) {
            partner.setContractEndDate(request.getContractEndDate());
        }
        if (request.getSecurityDeposit() != null) {
            partner.setSecurityDeposit(request.getSecurityDeposit());
        }
        if (request.getBankAccountNumber() != null) {
            partner.setBankAccountNumber(request.getBankAccountNumber());
        }
        if (request.getBankName() != null) {
            partner.setBankName(request.getBankName());
        }
        if (request.getBankBranch() != null) {
            partner.setBankBranch(request.getBankBranch());
        }

        partner = partnerRepository.save(partner);
        log.info("Partner updated successfully: {}", partnerId);

        return PartnerResponse.fromEntity(partner);
    }

    /**
     * Update partner status
     */
    @Transactional
    public PartnerResponse updateStatus(String partnerId, PartnerStatusUpdateRequest request, String updatedBy) {
        log.info("Updating partner status: {} to {}", partnerId, request.getStatus());

        PodPartner partner = findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found with id: " + partnerId));

        PartnerStatus currentStatus = partner.getStatus();
        PartnerStatus newStatus = request.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        partner.setStatus(newStatus);

        String[] credentials = null;

        // Handle specific status transitions
        if (newStatus == PartnerStatus.APPROVED || newStatus == PartnerStatus.ACTIVE) {
            partner.setApprovedAt(Instant.now());
            partner.setApprovedBy(updatedBy);

            // Create user account if transitioning to ACTIVE and no user exists
            if (newStatus == PartnerStatus.ACTIVE && partner.getUserId() == null) {
                credentials = createPartnerUserAccount(partner);
            }
        }

        partner = partnerRepository.save(partner);
        log.info("Partner status updated successfully: {} -> {}", currentStatus, newStatus);

        PartnerResponse response = PartnerResponse.fromEntity(partner);

        // Set username (always, if user exists)
        if (partner.getUserId() != null) {
            userRepository.findById(partner.getUserId()).ifPresent(user ->
                    response.setUsername(user.getUsername()));
        }

        // Set generated credentials (one-time, only when account was just created)
        if (credentials != null) {
            response.setGeneratedUsername(credentials[0]);
            response.setGeneratedPassword(credentials[1]);
        }

        return response;
    }

    /**
     * Get partner by ID
     */
    @Transactional(readOnly = true)
    public PartnerResponse getPartner(String partnerId) {
        PodPartner partner = findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found with id: " + partnerId));
        return PartnerResponse.fromEntity(partner);
    }

    /**
     * Get partner detail with statistics
     */
    @Transactional(readOnly = true)
    public PartnerDetailResponse getPartnerDetail(String partnerId) {
        PodPartner partner = findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found with id: " + partnerId));

        PartnerDetailResponse response = PartnerDetailResponse.fromEntity(partner);

        // Set username from User entity
        if (partner.getUserId() != null) {
            userRepository.findById(partner.getUserId()).ifPresent(user ->
                    response.setUsername(user.getUsername()));
        }

        // Calculate statistics
        long totalPods = podRepository.countByPartnerId(partnerId);
        long activePods = podRepository.findByPartnerIdAndStatus(partnerId, PodStatus.ACTIVE).size();
        long totalScans = scanRepository.countByPartnerId(partnerId);
        long totalAttributions = attributionRepository.countByPartnerId(partnerId);
        BigDecimal totalRevenue = attributionRepository.sumAttributedAmountByPartnerId(partnerId);
        BigDecimal paidCommissions = commissionRepository.sumPaidAmountByPartnerId(partnerId);
        BigDecimal pendingCommissions = commissionRepository.sumPendingAmountByPartnerId(partnerId);

        return response.withStatistics(
                (int) totalPods,
                (int) activePods,
                totalScans,
                totalAttributions,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                paidCommissions != null ? paidCommissions : BigDecimal.ZERO,
                pendingCommissions != null ? pendingCommissions : BigDecimal.ZERO
        );
    }

    /**
     * Search partners with criteria
     */
    @Transactional(readOnly = true)
    public Page<PartnerResponse> searchPartners(PartnerSearchCriteria criteria, Pageable pageable) {
        Specification<PodPartner> spec = buildSearchSpecification(criteria);
        return partnerRepository.findAll(spec, pageable).map(PartnerResponse::fromEntity);
    }

    /**
     * Get all active partners with pagination
     */
    @Transactional(readOnly = true)
    public Page<PartnerResponse> getAllPartners(Pageable pageable) {
        return partnerRepository.findAllActive(pageable).map(PartnerResponse::fromEntity);
    }

    /**
     * Get partner by user ID
     */
    @Transactional(readOnly = true)
    public Optional<PodPartner> findByUserId(Long userId) {
        return partnerRepository.findByUserId(userId);
    }

    /**
     * Delete partner (soft delete)
     */
    @Transactional
    public void deletePartner(String partnerId) {
        log.info("Deleting partner: {}", partnerId);
        softDeleteById(partnerId);
        log.info("Partner deleted successfully: {}", partnerId);
    }

    @Override
    public PodPartner update(String id, PodPartner entity) {
        PodPartner existing = findActiveById(id)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found with id: " + id));

        // Copy all fields
        existing.setBusinessName(entity.getBusinessName());
        existing.setBusinessType(entity.getBusinessType());
        existing.setContactName(entity.getContactName());
        existing.setContactEmail(entity.getContactEmail());
        existing.setContactPhone(entity.getContactPhone());
        existing.setBusinessLicense(entity.getBusinessLicense());
        existing.setTaxId(entity.getTaxId());
        existing.setAddressLine1(entity.getAddressLine1());
        existing.setAddressLine2(entity.getAddressLine2());
        existing.setCity(entity.getCity());
        existing.setState(entity.getState());
        existing.setPostalCode(entity.getPostalCode());
        existing.setCountry(entity.getCountry());
        existing.setTier(entity.getTier());
        existing.setCommissionRate(entity.getCommissionRate());
        existing.setNotes(entity.getNotes());

        return partnerRepository.save(existing);
    }

    // ========== Private Helper Methods ==========

    private void validateStatusTransition(PartnerStatus from, PartnerStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == PartnerStatus.APPROVED || to == PartnerStatus.TERMINATED;
            case APPROVED -> to == PartnerStatus.ACTIVE || to == PartnerStatus.TERMINATED;
            case ACTIVE -> to == PartnerStatus.SUSPENDED || to == PartnerStatus.TERMINATED;
            case SUSPENDED -> to == PartnerStatus.ACTIVE || to == PartnerStatus.TERMINATED;
            case TERMINATED -> false; // Terminal state
        };

        if (!valid) {
            throw new InvalidPartnerStatusTransitionException(
                    "Invalid status transition from " + from + " to " + to);
        }
    }

    /**
     * Creates a user account for the partner.
     * @return String[] with [username, tempPassword], or null if existing user was linked
     */
    private String[] createPartnerUserAccount(PodPartner partner) {
        log.info("Creating user account for partner: {}", partner.getId());

        // Check if user already exists with this email
        if (userRepository.existsByEmail(partner.getContactEmail())) {
            User existingUser = userRepository.findByEmail(partner.getContactEmail())
                    .orElseThrow();
            partner.setUserId(existingUser.getId());
            // Add PARTNER role to existing user
            Role partnerRole = roleRepository.findByName("PARTNER")
                    .orElseThrow(() -> new RuntimeException("PARTNER role not found"));
            existingUser.getRoles().add(partnerRole);
            userRepository.save(existingUser);
            log.info("Added PARTNER role to existing user: {}", existingUser.getId());
            return null;
        }

        // Create new user account
        Role partnerRole = roleRepository.findByName("PARTNER")
                .orElseThrow(() -> new RuntimeException("PARTNER role not found"));

        // Generate unique username from email + partner ID
        String baseUsername = partner.getContactEmail().split("@")[0] + "_partner";
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + suffix;
            suffix++;
        }

        // Generate temporary password
        String tempPassword = generateTempPassword();

        User user = User.builder()
                .username(username)
                .email(partner.getContactEmail())
                .password(passwordEncoder.encode(tempPassword))
                .firstName(partner.getContactName().split(" ")[0])
                .lastName(partner.getContactName().contains(" ") ?
                        partner.getContactName().substring(partner.getContactName().indexOf(" ") + 1) : "")
                .phoneNumber(partner.getContactPhone())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(Set.of(partnerRole))
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();

        user = userRepository.save(user);
        partner.setUserId(user.getId());

        log.info("User account created for partner. User ID: {}, Username: {}", user.getId(), username);
        return new String[]{username, tempPassword};
    }

    private String generateTempPassword() {
        // Generate a simple temporary password
        return "Partner@" + System.currentTimeMillis() % 10000;
    }

    private Specification<PodPartner> buildSearchSpecification(PartnerSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String keyword = "%" + criteria.getKeyword().toLowerCase() + "%";
                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("businessName")), keyword),
                        cb.like(cb.lower(root.get("contactEmail")), keyword),
                        cb.like(cb.lower(root.get("contactName")), keyword)
                );
                predicates.add(keywordPredicate);
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getTier() != null) {
                predicates.add(cb.equal(root.get("tier"), criteria.getTier()));
            }

            if (criteria.getBusinessType() != null) {
                predicates.add(cb.equal(root.get("businessType"), criteria.getBusinessType()));
            }

            if (criteria.getPartnerType() != null) {
                predicates.add(cb.equal(root.get("partnerType"), criteria.getPartnerType()));
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
}
