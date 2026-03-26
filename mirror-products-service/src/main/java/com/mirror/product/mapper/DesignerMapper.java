package com.mirror.product.mapper;

import com.mirror.product.dto.DesignerRequest;
import com.mirror.product.dto.DesignerResponse;
import com.mirror.product.entity.Designer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DesignerMapper {

    public Designer toEntity(DesignerRequest request) {
        if (request == null) {
            return null;
        }

        return Designer.builder()
                .code(request.getCode())
                .name(request.getName())
                .brandName(request.getBrandName())
                .specialty(request.getSpecialty())
                .yearsExperience(request.getYearsExperience())
                .designStyle(request.getDesignStyle())
                .defaultCommissionPercent(request.getDefaultCommissionPercent())
                .defaultLoyaltyPercent(request.getDefaultLoyaltyPercent())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .website(request.getWebsite())
                .socialMediaLinks(request.getSocialMediaLinks())
                .bio(request.getBio())
                .portfolioUrl(request.getPortfolioUrl())
                .verified(request.getVerified() != null ? request.getVerified() : false)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .isActive(true)
                .build();
    }

    public DesignerResponse toResponse(Designer entity) {
        if (entity == null) {
            return null;
        }

        return DesignerResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .ownerUserId(entity.getOwnerUserId())
                .brandName(entity.getBrandName())
                .specialty(entity.getSpecialty())
                .yearsExperience(entity.getYearsExperience())
                .designStyle(entity.getDesignStyle())
                .defaultCommissionPercent(entity.getDefaultCommissionPercent())
                .defaultLoyaltyPercent(entity.getDefaultLoyaltyPercent())
                .totalDesignsCreated(entity.getTotalDesignsCreated())
                .totalProductsSold(entity.getTotalProductsSold())
                .totalEarnings(entity.getTotalEarnings())
                .joinDate(entity.getJoinDate())
                .lastDesignDate(entity.getLastDesignDate())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .website(entity.getWebsite())
                .socialMediaLinks(entity.getSocialMediaLinks())
                .bio(entity.getBio())
                .portfolioUrl(entity.getPortfolioUrl())
                .verified(entity.getVerified())
                .featured(entity.getFeatured())
                .rating(entity.getRating())
                .reviewCount(entity.getReviewCount())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .designProductCount(entity.getDesignProducts() != null ? entity.getDesignProducts().size() : 0)
                .build();
    }

    public List<DesignerResponse> toResponseList(List<Designer> designers) {
        if (designers == null) {
            return null;
        }

        return designers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntity(Designer entity, DesignerRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCode() != null) {
            entity.setCode(request.getCode());
        }
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getBrandName() != null) {
            entity.setBrandName(request.getBrandName());
        }
        if (request.getSpecialty() != null) {
            entity.setSpecialty(request.getSpecialty());
        }
        if (request.getYearsExperience() != null) {
            entity.setYearsExperience(request.getYearsExperience());
        }
        if (request.getDesignStyle() != null) {
            entity.setDesignStyle(request.getDesignStyle());
        }
        if (request.getDefaultCommissionPercent() != null) {
            entity.setDefaultCommissionPercent(request.getDefaultCommissionPercent());
        }
        if (request.getDefaultLoyaltyPercent() != null) {
            entity.setDefaultLoyaltyPercent(request.getDefaultLoyaltyPercent());
        }
        if (request.getContactEmail() != null) {
            entity.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            entity.setContactPhone(request.getContactPhone());
        }
        if (request.getWebsite() != null) {
            entity.setWebsite(request.getWebsite());
        }
        if (request.getSocialMediaLinks() != null) {
            entity.setSocialMediaLinks(request.getSocialMediaLinks());
        }
        if (request.getBio() != null) {
            entity.setBio(request.getBio());
        }
        if (request.getPortfolioUrl() != null) {
            entity.setPortfolioUrl(request.getPortfolioUrl());
        }
        if (request.getVerified() != null) {
            entity.setVerified(request.getVerified());
        }
        if (request.getFeatured() != null) {
            entity.setFeatured(request.getFeatured());
        }
    }
}