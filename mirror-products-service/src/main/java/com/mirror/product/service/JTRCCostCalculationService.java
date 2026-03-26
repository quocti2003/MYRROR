package com.mirror.product.service;

import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.entity.JTRCLaborComponent;
import com.mirror.product.entity.JTRCMetalComponent;
import com.mirror.product.entity.JTRCStoneComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for calculating costs in JTRC (Jewelry Technical Report Card)
 * Handles metal, stone, labor, and total COGS calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JTRCCostCalculationService {

    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculate metal cost for a metal component
     * Formula: (weight × pricePerGram) + (weight × lossRate × pricePerGram)
     * = weight × pricePerGram × (1 + lossRate/100)
     *
     * @param component The metal component to calculate
     */
    public void calculateMetalCost(JTRCMetalComponent component) {
        if (component == null) {
            return;
        }

        BigDecimal weight = component.getWeightGrams();
        BigDecimal pricePerGram = component.getPricePerGram();
        BigDecimal lossRatePercent = component.getLossRatePercent();

        if (weight == null || pricePerGram == null) {
            component.setMetalCost(BigDecimal.ZERO);
            component.setLossCost(BigDecimal.ZERO);
            component.setTotalCost(BigDecimal.ZERO);
            return;
        }

        // Calculate base metal cost
        BigDecimal metalCost = weight.multiply(pricePerGram)
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);
        component.setMetalCost(metalCost);

        // Calculate loss cost
        BigDecimal lossCost = BigDecimal.ZERO;
        if (lossRatePercent != null && lossRatePercent.compareTo(BigDecimal.ZERO) > 0) {
            lossCost = metalCost.multiply(lossRatePercent)
                    .divide(BigDecimal.valueOf(100), CURRENCY_SCALE, ROUNDING_MODE);
        }
        component.setLossCost(lossCost);

        // Calculate total cost
        BigDecimal totalCost = metalCost.add(lossCost);
        component.setTotalCost(totalCost);

        log.debug("Calculated metal cost: metalCost={}, lossCost={}, totalCost={}",
                metalCost, lossCost, totalCost);
    }

    /**
     * Calculate total cost for a stone component
     * Formula: quantity × unitPrice
     *
     * @param component The stone component to calculate
     */
    public void calculateStoneCost(JTRCStoneComponent component) {
        if (component == null) {
            return;
        }

        Integer quantity = component.getQuantity();
        BigDecimal unitPrice = component.getUnitPrice();

        if (quantity == null || unitPrice == null) {
            component.setTotalPrice(BigDecimal.ZERO);
            return;
        }

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);
        component.setTotalPrice(totalPrice);

        log.debug("Calculated stone cost: quantity={}, unitPrice={}, totalPrice={}",
                quantity, unitPrice, totalPrice);
    }

    /**
     * Calculate total stone cost for a JTRC
     * Formula: sum of (quantity × unitPrice) for all stones
     *
     * @param jtrc The JTRC to calculate stone costs for
     * @return Total stone cost
     */
    public BigDecimal calculateTotalStoneCost(JewelryTechnicalReport jtrc) {
        if (jtrc == null || jtrc.getStoneComponents() == null || jtrc.getStoneComponents().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalStoneCost = BigDecimal.ZERO;
        for (JTRCStoneComponent stone : jtrc.getStoneComponents()) {
            calculateStoneCost(stone); // Ensure individual stone costs are calculated
            if (stone.getTotalPrice() != null) {
                totalStoneCost = totalStoneCost.add(stone.getTotalPrice());
            }
        }

        return totalStoneCost.setScale(CURRENCY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate total labor cost for a JTRC
     * Formula: sum of all labor component costs
     *
     * @param jtrc The JTRC to calculate labor costs for
     * @return Total labor cost
     */
    public BigDecimal calculateTotalLaborCost(JewelryTechnicalReport jtrc) {
        if (jtrc == null || jtrc.getLaborComponents() == null || jtrc.getLaborComponents().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalLaborCost = BigDecimal.ZERO;
        for (JTRCLaborComponent labor : jtrc.getLaborComponents()) {
            if (labor.getCost() != null) {
                totalLaborCost = totalLaborCost.add(labor.getCost());
            }
        }

        return totalLaborCost.setScale(CURRENCY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate total metal cost for a JTRC
     *
     * @param jtrc The JTRC to calculate metal cost for
     * @return Total metal cost
     */
    public BigDecimal calculateTotalMetalCost(JewelryTechnicalReport jtrc) {
        if (jtrc == null || jtrc.getMetalComponent() == null) {
            return BigDecimal.ZERO;
        }

        JTRCMetalComponent metalComponent = jtrc.getMetalComponent();
        calculateMetalCost(metalComponent); // Ensure metal costs are calculated

        if (metalComponent.getTotalCost() != null) {
            return metalComponent.getTotalCost();
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calculate total COGS (Cost of Goods Sold) in VND
     * Formula: metal + stone + labor costs
     *
     * @param jtrc The JTRC to calculate COGS for
     * @return Total COGS in VND
     */
    public BigDecimal calculateTotalCOGS(JewelryTechnicalReport jtrc) {
        if (jtrc == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal metalCost = calculateTotalMetalCost(jtrc);
        BigDecimal stoneCost = calculateTotalStoneCost(jtrc);
        BigDecimal laborCost = calculateTotalLaborCost(jtrc);

        BigDecimal totalCOGS = metalCost.add(stoneCost).add(laborCost)
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        log.debug("Calculated total COGS: metalCost={}, stoneCost={}, laborCost={}, totalCOGS={}",
                metalCost, stoneCost, laborCost, totalCOGS);

        return totalCOGS;
    }

    /**
     * Convert VND amount to USD using the exchange rate
     *
     * @param amountVnd The amount in VND
     * @param exchangeRate The VND to USD exchange rate
     * @return The amount in USD
     */
    public BigDecimal convertToUSD(BigDecimal amountVnd, BigDecimal exchangeRate) {
        if (amountVnd == null || exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return amountVnd.divide(exchangeRate, CURRENCY_SCALE, ROUNDING_MODE);
    }

    /**
     * Recalculate all costs for a JTRC
     * This method updates the JTRC entity with all calculated values
     *
     * @param jtrc The JTRC to recalculate
     */
    public void recalculateAllCosts(JewelryTechnicalReport jtrc) {
        if (jtrc == null) {
            return;
        }

        log.info("Recalculating all costs for JTRC: {}", jtrc.getId());

        // Calculate individual component costs
        if (jtrc.getMetalComponent() != null) {
            calculateMetalCost(jtrc.getMetalComponent());
        }

        if (jtrc.getStoneComponents() != null) {
            for (JTRCStoneComponent stone : jtrc.getStoneComponents()) {
                calculateStoneCost(stone);
            }
        }

        // Calculate totals
        BigDecimal totalMetalCost = calculateTotalMetalCost(jtrc);
        BigDecimal totalStoneCost = calculateTotalStoneCost(jtrc);
        BigDecimal totalLaborCost = calculateTotalLaborCost(jtrc);
        BigDecimal totalCogsVnd = totalMetalCost.add(totalStoneCost).add(totalLaborCost)
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        // Update JTRC
        jtrc.setTotalMetalCost(totalMetalCost);
        jtrc.setTotalStoneCost(totalStoneCost);
        jtrc.setTotalLaborCost(totalLaborCost);
        jtrc.setTotalCogsVnd(totalCogsVnd);

        // Convert to USD if exchange rate is available
        if (jtrc.getExchangeRateUsd() != null && jtrc.getExchangeRateUsd().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalCogsUsd = convertToUSD(totalCogsVnd, jtrc.getExchangeRateUsd());
            jtrc.setTotalCogsUsd(totalCogsUsd);
        } else {
            jtrc.setTotalCogsUsd(BigDecimal.ZERO);
        }

        log.info("Recalculated costs for JTRC {}: metalCost={}, stoneCost={}, laborCost={}, totalCogsVnd={}, totalCogsUsd={}",
                jtrc.getId(), totalMetalCost, totalStoneCost, totalLaborCost, jtrc.getTotalCogsVnd(), jtrc.getTotalCogsUsd());
    }

    /**
     * Validate that costs are properly calculated
     *
     * @param jtrc The JTRC to validate
     * @return true if costs are valid, false otherwise
     */
    public boolean validateCosts(JewelryTechnicalReport jtrc) {
        if (jtrc == null) {
            return false;
        }

        // Check that total COGS equals sum of components
        BigDecimal expectedTotal = BigDecimal.ZERO;

        if (jtrc.getTotalMetalCost() != null) {
            expectedTotal = expectedTotal.add(jtrc.getTotalMetalCost());
        }
        if (jtrc.getTotalStoneCost() != null) {
            expectedTotal = expectedTotal.add(jtrc.getTotalStoneCost());
        }
        if (jtrc.getTotalLaborCost() != null) {
            expectedTotal = expectedTotal.add(jtrc.getTotalLaborCost());
        }

        expectedTotal = expectedTotal.setScale(CURRENCY_SCALE, ROUNDING_MODE);

        BigDecimal actualTotal = jtrc.getTotalCogsVnd();
        if (actualTotal == null) {
            actualTotal = BigDecimal.ZERO;
        }
        actualTotal = actualTotal.setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return expectedTotal.compareTo(actualTotal) == 0;
    }
}
