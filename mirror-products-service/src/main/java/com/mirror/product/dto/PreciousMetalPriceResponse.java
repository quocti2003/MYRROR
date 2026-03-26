package com.mirror.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PreciousMetalPriceResponse {
    private String metalType; // GOLD, SILVER, PLATINUM, PALLADIUM
    private BigDecimal pricePerOunce;
    private BigDecimal pricePerGram;
    private String currency; // USD or VND
    private BigDecimal exchangeRate; // If VND, the USD->VND rate used
    private String source; // API source
    private LocalDateTime timestamp;
    private BigDecimal change24h; // Percentage change in 24 hours
    private BigDecimal changePercent24h;

    public PreciousMetalPriceResponse() {}

    // Getters and setters
    public String getMetalType() {
        return metalType;
    }

    public void setMetalType(String metalType) {
        this.metalType = metalType;
    }

    public BigDecimal getPricePerOunce() {
        return pricePerOunce;
    }

    public void setPricePerOunce(BigDecimal pricePerOunce) {
        this.pricePerOunce = pricePerOunce;
    }

    public BigDecimal getPricePerGram() {
        return pricePerGram;
    }

    public void setPricePerGram(BigDecimal pricePerGram) {
        this.pricePerGram = pricePerGram;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getChange24h() {
        return change24h;
    }

    public void setChange24h(BigDecimal change24h) {
        this.change24h = change24h;
    }

    public BigDecimal getChangePercent24h() {
        return changePercent24h;
    }

    public void setChangePercent24h(BigDecimal changePercent24h) {
        this.changePercent24h = changePercent24h;
    }
}
