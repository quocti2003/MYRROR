package com.mirror.product.service;

import com.mirror.product.dto.DesignerDashboardResponse;
import com.mirror.product.dto.DesignProductResponse;
import com.mirror.product.dto.DesignSaleTransactionResponse;
import com.mirror.product.entity.Designer;
import com.mirror.product.entity.DesignProduct;
import com.mirror.product.entity.DesignSaleTransaction;
import com.mirror.product.repository.DesignerRepository;
import com.mirror.product.repository.DesignProductRepository;
import com.mirror.product.repository.DesignSaleTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DesignerService extends BaseService<Designer, String> {

    private final DesignerRepository designerRepository;
    private final DesignProductRepository designProductRepository;
    private final DesignSaleTransactionRepository designSaleTransactionRepository;

    public DesignerService(DesignerRepository designerRepository,
                          DesignProductRepository designProductRepository,
                          DesignSaleTransactionRepository designSaleTransactionRepository) {
        super(designerRepository);
        this.designerRepository = designerRepository;
        this.designProductRepository = designProductRepository;
        this.designSaleTransactionRepository = designSaleTransactionRepository;
    }

    public Optional<Designer> findByCode(String code) {
        return designerRepository.findActiveByCode(code);
    }

    public boolean existsByCode(String code) {
        return designerRepository.existsActiveByCode(code);
    }

    public List<Designer> findByOwnerUserId(String ownerUserId) {
        return designerRepository.findActiveByOwnerUserId(ownerUserId);
    }

    public List<Designer> findBySpecialty(String specialty) {
        return designerRepository.findActiveBySpecialty(specialty);
    }

    public List<Designer> findVerifiedDesigners() {
        return designerRepository.findActiveVerifiedDesigners();
    }

    public List<Designer> findFeaturedDesigners() {
        return designerRepository.findActiveFeaturedDesigners();
    }

    public Page<Designer> searchDesigners(String search, Pageable pageable) {
        return designerRepository.searchActiveDesigners(search, pageable);
    }

    @Override
    public Designer update(String designerId, Designer designerDetails) {
        Designer designer = findActiveById(designerId)
                .orElseThrow(() -> new RuntimeException("Designer not found with id: " + designerId));

        // Validate unique designer code for update
        if (!designer.getCode().equals(designerDetails.getCode()) &&
            designerRepository.existsActiveByCodeAndNotId(designerDetails.getCode(), designerId)) {
            throw new IllegalArgumentException("Designer code already exists: " + designerDetails.getCode());
        }

        designer.setCode(designerDetails.getCode());
        designer.setName(designerDetails.getName());
        designer.setBrandName(designerDetails.getBrandName());
        designer.setSpecialty(designerDetails.getSpecialty());
        designer.setYearsExperience(designerDetails.getYearsExperience());
        designer.setDesignStyle(designerDetails.getDesignStyle());
        designer.setDefaultCommissionPercent(designerDetails.getDefaultCommissionPercent());
        designer.setDefaultLoyaltyPercent(designerDetails.getDefaultLoyaltyPercent());
        designer.setContactEmail(designerDetails.getContactEmail());
        designer.setContactPhone(designerDetails.getContactPhone());
        designer.setWebsite(designerDetails.getWebsite());
        designer.setSocialMediaLinks(designerDetails.getSocialMediaLinks());
        designer.setBio(designerDetails.getBio());
        designer.setPortfolioUrl(designerDetails.getPortfolioUrl());
        designer.setVerified(designerDetails.getVerified());
        designer.setFeatured(designerDetails.getFeatured());

        return save(designer);
    }

    @Override
    protected void validateBeforeSave(Designer designer) {
        if (designer.getId() == null || designer.getId().isEmpty()) {
            // Only validate for new designers (no ID)
            if (designerRepository.existsActiveByCode(designer.getCode())) {
                throw new IllegalArgumentException("Designer code already exists: " + designer.getCode());
            }
        }
    }

    @Override
    public Designer save(Designer designer) {
        if (designer.getId() == null || designer.getId().isEmpty()) {
            validateBeforeSave(designer);
        } else {
            // For updates, validate only if designer code changed
            Designer existing = findActiveById(designer.getId()).orElse(null);
            if (existing != null && !existing.getCode().equals(designer.getCode())) {
                if (designerRepository.existsActiveByCodeAndNotId(designer.getCode(), designer.getId())) {
                    throw new IllegalArgumentException("Designer code already exists: " + designer.getCode());
                }
            }
        }
        return super.save(designer);
    }

    @Override
    protected void validateBeforeDelete(String designerId) {
        // Check if designer has any design products
        long designProductCount = designProductRepository.countActiveDesignsByDesignerId(designerId);
        if (designProductCount > 0) {
            throw new IllegalStateException("Cannot delete designer. There are " + designProductCount + " design products associated with this designer.");
        }
    }

    // Dashboard specific methods
    public DesignerDashboardResponse getDashboardData(String designerId) {
        Designer designer = findActiveById(designerId)
                .orElseThrow(() -> new RuntimeException("Designer not found with id: " + designerId));

        return buildDashboardResponse(designer);
    }

    public DesignerDashboardResponse getDashboardDataByUserId(String userId) {
        List<Designer> designers = findByOwnerUserId(userId);
        if (designers.isEmpty()) {
            throw new RuntimeException("No designer found for user id: " + userId);
        }

        // For now, take the first designer. In the future, we might support multiple designers per user
        Designer designer = designers.get(0);
        return buildDashboardResponse(designer);
    }

    private DesignerDashboardResponse buildDashboardResponse(Designer designer) {
        String designerId = designer.getId();

        // Calculate aggregated statistics
        BigDecimal totalSalesAmount = designSaleTransactionRepository.getTotalSalesAmountByDesignerId(designerId);
        BigDecimal totalEarnings = designSaleTransactionRepository.getTotalEarningsByDesignerId(designerId);
        Long totalSalesCount = designSaleTransactionRepository.getTotalSalesCountByDesignerId(designerId);
        Long activeDesignsCount = designProductRepository.countActiveDesignsByDesignerId(designerId);

        // Monthly statistics
        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59);

        BigDecimal currentMonthSales = designSaleTransactionRepository.getMonthlySalesAmountByDesignerId(designerId, startOfMonth, endOfMonth);
        BigDecimal currentMonthEarnings = designSaleTransactionRepository.getMonthlyEarningsByDesignerId(designerId, startOfMonth, endOfMonth);

        // Rating statistics
        BigDecimal averageRating = designSaleTransactionRepository.getAverageRatingByDesignerId(designerId);
        Long totalReviewsCount = designSaleTransactionRepository.getRatingCountByDesignerId(designerId);

        // Recent sales (last 10)
        List<DesignSaleTransaction> recentSalesEntities = designSaleTransactionRepository
                .findActiveByDesignerIdPageable(designerId, PageRequest.of(0, 10))
                .getContent();
        List<DesignSaleTransactionResponse> recentSales = recentSalesEntities.stream()
                .map(DesignSaleTransactionResponse::new)
                .collect(Collectors.toList());

        // Top designs by sales amount (last 5)
        List<DesignProduct> topDesignsEntities = designProductRepository
                .findTopDesignsByDesignerId(designerId, PageRequest.of(0, 5))
                .getContent();
        List<DesignProductResponse> topDesigns = topDesignsEntities.stream()
                .map(DesignProductResponse::new)
                .collect(Collectors.toList());

        // Active commissions (designs with commission percentages set)
        List<DesignProduct> activeCommissionsEntities = designProductRepository
                .findActiveByDesignerIdAndStatus(designerId, "ACTIVE");
        List<DesignProductResponse> activeCommissions = activeCommissionsEntities.stream()
                .filter(dp -> dp.getCommissionPercentage() != null && dp.getCommissionPercentage().compareTo(BigDecimal.ZERO) > 0)
                .map(DesignProductResponse::new)
                .collect(Collectors.toList());

        // Calculate average order value
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalSalesCount != null && totalSalesCount > 0 && totalSalesAmount != null) {
            averageOrderValue = totalSalesAmount.divide(BigDecimal.valueOf(totalSalesCount), 2, BigDecimal.ROUND_HALF_UP);
        }

        return DesignerDashboardResponse.builder()
                .designerId(designerId)
                .designerName(designer.getName())
                .brandName(designer.getBrandName())
                .totalSalesAmount(totalSalesAmount != null ? totalSalesAmount : BigDecimal.ZERO)
                .totalSalesCount(totalSalesCount != null ? totalSalesCount.intValue() : 0)
                .totalEarnings(totalEarnings != null ? totalEarnings : BigDecimal.ZERO)
                .activeDesignsCount(activeDesignsCount != null ? activeDesignsCount.intValue() : 0)
                .totalDesignsCreated(designer.getTotalDesignsCreated() != null ? designer.getTotalDesignsCreated() : 0)
                .currentMonthSales(currentMonthSales != null ? currentMonthSales : BigDecimal.ZERO)
                .currentMonthEarnings(currentMonthEarnings != null ? currentMonthEarnings : BigDecimal.ZERO)
                .averageRating(averageRating)
                .totalReviewsCount(totalReviewsCount != null ? totalReviewsCount.intValue() : 0)
                .averageOrderValue(averageOrderValue)
                .recentSales(recentSales)
                .topDesigns(topDesigns)
                .activeCommissions(activeCommissions)
                .build();
    }
}