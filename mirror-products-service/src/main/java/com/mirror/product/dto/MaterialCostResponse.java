package com.mirror.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaterialCostResponse {
    private String metalType;
    private BigDecimal weightInGrams;
    private BigDecimal pricePerGram;
    private BigDecimal costPerPiece;
    private BigDecimal totalCost;
    private Integer quantity;
    private String currency;
    private LocalDateTime calculatedAt;
    private String priceSource;

    public MaterialCostResponse() {}

    // Getters and setters
    public String getMetalType() {
        return metalType;
    }

    public void setMetalType(String metalType) {
        this.metalType = metalType;
    }

    public BigDecimal getWeightInGrams() {
        return weightInGrams;
    }

    public void setWeightInGrams(BigDecimal weightInGrams) {
        this.weightInGrams = weightInGrams;
    }

    public BigDecimal getPricePerGram() {
        return pricePerGram;
    }

    public void setPricePerGram(BigDecimal pricePerGram) {
        this.pricePerGram = pricePerGram;
    }

    public BigDecimal getCostPerPiece() {
        return costPerPiece;
    }

    public void setCostPerPiece(BigDecimal costPerPiece) {
        this.costPerPiece = costPerPiece;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(String priceSource) {
        this.priceSource = priceSource;
    }
}
