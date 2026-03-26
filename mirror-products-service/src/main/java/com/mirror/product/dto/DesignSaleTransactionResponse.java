package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.entity.DesignSaleTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DesignSaleTransactionResponse {

    private String id;
    private String designProductId;
    private String designId;
    private String designName;
    private String productId;
    private String productName;
    private String orderId;
    private BigDecimal saleAmount;
    private BigDecimal commissionAmount;
    private BigDecimal loyaltyAmount;
    private BigDecimal commissionPercentage;
    private BigDecimal loyaltyPercentage;
    private String currency;
    private Integer quantity;
    private LocalDateTime saleDate;
    private Boolean processed;
    private BigDecimal customerRating;
    private String customerReview;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public DesignSaleTransactionResponse(DesignSaleTransaction transaction) {
        this.id = transaction.getId();
        this.designProductId = transaction.getDesignProduct() != null ? transaction.getDesignProduct().getId() : null;
        this.designId = transaction.getDesignProduct() != null ? transaction.getDesignProduct().getId() : null;
        this.designName = transaction.getDesignProduct() != null ? transaction.getDesignProduct().getDesignName() : null;
        this.productId = transaction.getDesignProduct() != null && transaction.getDesignProduct().getProduct() != null
                ? transaction.getDesignProduct().getProduct().getId() : null;
        this.productName = transaction.getDesignProduct() != null && transaction.getDesignProduct().getProduct() != null
                ? transaction.getDesignProduct().getProduct().getItemName() : null;
        this.orderId = transaction.getOrderId();
        this.saleAmount = transaction.getSaleAmount();
        this.commissionAmount = transaction.getCommissionAmount();
        this.loyaltyAmount = transaction.getLoyaltyAmount();
        this.commissionPercentage = transaction.getCommissionPercentage();
        this.loyaltyPercentage = transaction.getLoyaltyPercentage();
        this.currency = transaction.getCurrency();
        this.quantity = transaction.getQuantity();
        this.saleDate = transaction.getSaleDate();
        this.processed = transaction.getProcessed();
        this.customerRating = transaction.getCustomerRating();
        this.customerReview = transaction.getCustomerReview();
        this.isActive = transaction.getIsActive();
        this.createdAt = transaction.getCreatedAt();
        this.updatedAt = transaction.getUpdatedAt();
    }

    public BigDecimal getTotalEarnings() {
        BigDecimal commission = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        BigDecimal loyalty = loyaltyAmount != null ? loyaltyAmount : BigDecimal.ZERO;
        return commission.add(loyalty);
    }
}