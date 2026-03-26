package com.mirror.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ImportCostResponse {
    private BigDecimal subtotal;
    private BigDecimal customsDuty;
    private BigDecimal vat;
    private BigDecimal shippingCost;
    private BigDecimal insuranceCost;
    private BigDecimal totalCost;
    private BigDecimal costPerUnit;
    private String currency;
    private BigDecimal exchangeRate;
    private String exchangeRateSource;
    private LocalDateTime calculatedAt;

    public ImportCostResponse() {}

    // Getters and setters
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getCustomsDuty() {
        return customsDuty;
    }

    public void setCustomsDuty(BigDecimal customsDuty) {
        this.customsDuty = customsDuty;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public void setVat(BigDecimal vat) {
        this.vat = vat;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public BigDecimal getInsuranceCost() {
        return insuranceCost;
    }

    public void setInsuranceCost(BigDecimal insuranceCost) {
        this.insuranceCost = insuranceCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getExchangeRateSource() {
        return exchangeRateSource;
    }

    public void setExchangeRateSource(String exchangeRateSource) {
        this.exchangeRateSource = exchangeRateSource;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
