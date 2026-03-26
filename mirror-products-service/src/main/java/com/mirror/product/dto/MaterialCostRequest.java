package com.mirror.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class MaterialCostRequest {
    @NotBlank(message = "Metal type is required")
    private String metalType; // GOLD, SILVER, PLATINUM, PALLADIUM

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private BigDecimal weight;

    @NotBlank(message = "Weight unit is required")
    private String weightUnit; // GRAMS, OUNCES, LUONG, CHI

    @NotBlank(message = "Currency is required")
    private String currency; // USD or VND

    private Integer quantity; // Number of pieces (default 1)

    public MaterialCostRequest() {
        this.quantity = 1;
    }

    // Getters and setters
    public String getMetalType() {
        return metalType;
    }

    public void setMetalType(String metalType) {
        this.metalType = metalType;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
