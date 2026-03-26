package com.mirror.product.service;

import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.entity.JTRCLaborComponent;
import com.mirror.product.entity.JTRCMetalComponent;
import com.mirror.product.entity.JTRCStoneComponent;
import com.mirror.product.enums.JTRCLaborType;
import com.mirror.product.enums.JTRCStatus;
import com.mirror.product.enums.StoneColorCategory;
import com.mirror.product.enums.StoneRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive JUnit test suite for JTRCCostCalculationService
 * Tests all cost calculation methods including metal, stone, labor, and COGS calculations
 */
@DisplayName("JTRC Cost Calculation Service Tests")
class JTRCCostCalculationServiceTest {

    private JTRCCostCalculationService calculationService;

    // Test data constants
    private static final BigDecimal METAL_WEIGHT = new BigDecimal("5.5");
    private static final BigDecimal METAL_PRICE_PER_GRAM = new BigDecimal("1850000");
    private static final BigDecimal METAL_LOSS_RATE = new BigDecimal("3.5");
    private static final BigDecimal EXPECTED_METAL_COST = new BigDecimal("10175000.00");
    private static final BigDecimal EXPECTED_LOSS_COST = new BigDecimal("356125.00");
    private static final BigDecimal EXPECTED_TOTAL_METAL_COST = new BigDecimal("10531125.00");

    private static final Integer STONE1_QUANTITY = 1;
    private static final BigDecimal STONE1_UNIT_PRICE = new BigDecimal("25000000");
    private static final BigDecimal EXPECTED_STONE1_TOTAL = new BigDecimal("25000000.00");

    private static final Integer STONE2_QUANTITY = 12;
    private static final BigDecimal STONE2_UNIT_PRICE = new BigDecimal("500000");
    private static final BigDecimal EXPECTED_STONE2_TOTAL = new BigDecimal("6000000.00");

    private static final BigDecimal LABOR1_COST = new BigDecimal("2500000");
    private static final BigDecimal LABOR2_COST = new BigDecimal("1000000");
    private static final BigDecimal EXPECTED_TOTAL_LABOR_COST = new BigDecimal("3500000.00");

    private static final BigDecimal EXPECTED_TOTAL_COGS_VND = new BigDecimal("45031125.00");
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("24500");
    private static final BigDecimal EXPECTED_TOTAL_COGS_USD = new BigDecimal("1838.01");

    @BeforeEach
    void setUp() {
        calculationService = new JTRCCostCalculationService();
    }

    // ==================== Metal Cost Calculation Tests ====================

    @Test
    @DisplayName("Calculate metal cost with valid inputs - should calculate correctly")
    void calculateMetalCost_withValidInputs_calculatesCorrectly() {
        // Given
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            METAL_PRICE_PER_GRAM,
            METAL_LOSS_RATE
        );

        // When
        calculationService.calculateMetalCost(metalComponent);

        // Then
        assertThat(metalComponent.getMetalCost())
            .isEqualByComparingTo(EXPECTED_METAL_COST);
        assertThat(metalComponent.getLossCost())
            .isEqualByComparingTo(EXPECTED_LOSS_COST);
        assertThat(metalComponent.getTotalCost())
            .isEqualByComparingTo(EXPECTED_TOTAL_METAL_COST);
    }

    @Test
    @DisplayName("Calculate metal cost with null weight - should return zero")
    void calculateMetalCost_withNullWeight_returnsZero() {
        // Given
        JTRCMetalComponent metalComponent = createMetalComponent(
            null,
            METAL_PRICE_PER_GRAM,
            METAL_LOSS_RATE
        );

        // When
        calculationService.calculateMetalCost(metalComponent);

        // Then
        assertThat(metalComponent.getMetalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metalComponent.getLossCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metalComponent.getTotalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate metal cost with null price - should return zero")
    void calculateMetalCost_withNullPrice_returnsZero() {
        // Given
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            null,
            METAL_LOSS_RATE
        );

        // When
        calculationService.calculateMetalCost(metalComponent);

        // Then
        assertThat(metalComponent.getMetalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metalComponent.getLossCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metalComponent.getTotalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate metal cost with zero loss rate - should have no loss cost")
    void calculateMetalCost_withZeroLossRate_noLossCost() {
        // Given
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            METAL_PRICE_PER_GRAM,
            BigDecimal.ZERO
        );

        // When
        calculationService.calculateMetalCost(metalComponent);

        // Then
        assertThat(metalComponent.getMetalCost())
            .isEqualByComparingTo(EXPECTED_METAL_COST);
        assertThat(metalComponent.getLossCost())
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metalComponent.getTotalCost())
            .isEqualByComparingTo(EXPECTED_METAL_COST);
    }

    @Test
    @DisplayName("Calculate metal cost with high loss rate - should calculate loss cost correctly")
    void calculateMetalCost_withHighLossRate_calculatesLossCost() {
        // Given
        BigDecimal highLossRate = new BigDecimal("10.0");
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            METAL_PRICE_PER_GRAM,
            highLossRate
        );

        // When
        calculationService.calculateMetalCost(metalComponent);

        // Then
        BigDecimal expectedLossCost = EXPECTED_METAL_COST
            .multiply(highLossRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = EXPECTED_METAL_COST.add(expectedLossCost);

        assertThat(metalComponent.getMetalCost())
            .isEqualByComparingTo(EXPECTED_METAL_COST);
        assertThat(metalComponent.getLossCost())
            .isEqualByComparingTo(expectedLossCost);
        assertThat(metalComponent.getTotalCost())
            .isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Calculate metal cost with null component - should handle gracefully")
    void calculateMetalCost_withNullComponent_handlesGracefully() {
        // When/Then - should not throw exception
        calculationService.calculateMetalCost(null);
    }

    // ==================== Stone Cost Calculation Tests ====================

    @Test
    @DisplayName("Calculate stone cost with valid inputs - should calculate correctly")
    void calculateStoneCost_withValidInputs_calculatesCorrectly() {
        // Given
        JTRCStoneComponent stoneComponent = createStoneComponent(
            STONE1_QUANTITY,
            STONE1_UNIT_PRICE
        );

        // When
        calculationService.calculateStoneCost(stoneComponent);

        // Then
        assertThat(stoneComponent.getTotalPrice())
            .isEqualByComparingTo(EXPECTED_STONE1_TOTAL);
    }

    @Test
    @DisplayName("Calculate stone cost with null quantity - should return zero")
    void calculateStoneCost_withNullQuantity_returnsZero() {
        // Given
        JTRCStoneComponent stoneComponent = createStoneComponent(
            null,
            STONE1_UNIT_PRICE
        );

        // When
        calculationService.calculateStoneCost(stoneComponent);

        // Then
        assertThat(stoneComponent.getTotalPrice())
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate stone cost with null price - should return zero")
    void calculateStoneCost_withNullPrice_returnsZero() {
        // Given
        JTRCStoneComponent stoneComponent = createStoneComponent(
            STONE1_QUANTITY,
            null
        );

        // When
        calculationService.calculateStoneCost(stoneComponent);

        // Then
        assertThat(stoneComponent.getTotalPrice())
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate stone cost with multiple quantity - should multiply correctly")
    void calculateStoneCost_withMultipleQuantity_multipliesCorrectly() {
        // Given
        JTRCStoneComponent stoneComponent = createStoneComponent(
            STONE2_QUANTITY,
            STONE2_UNIT_PRICE
        );

        // When
        calculationService.calculateStoneCost(stoneComponent);

        // Then
        assertThat(stoneComponent.getTotalPrice())
            .isEqualByComparingTo(EXPECTED_STONE2_TOTAL);
    }

    @Test
    @DisplayName("Calculate stone cost with null component - should handle gracefully")
    void calculateStoneCost_withNullComponent_handlesGracefully() {
        // When/Then - should not throw exception
        calculationService.calculateStoneCost(null);
    }

    // ==================== Total Stone Cost Tests ====================

    @Test
    @DisplayName("Calculate total stone cost with multiple stones - should sum correctly")
    void calculateTotalStoneCost_withMultipleStones_sumsCorrectly() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        JTRCStoneComponent stone1 = createStoneComponent(STONE1_QUANTITY, STONE1_UNIT_PRICE);
        JTRCStoneComponent stone2 = createStoneComponent(STONE2_QUANTITY, STONE2_UNIT_PRICE);
        jtrc.addStoneComponent(stone1);
        jtrc.addStoneComponent(stone2);

        // When
        BigDecimal totalStoneCost = calculationService.calculateTotalStoneCost(jtrc);

        // Then
        BigDecimal expectedTotal = EXPECTED_STONE1_TOTAL.add(EXPECTED_STONE2_TOTAL);
        assertThat(totalStoneCost).isEqualByComparingTo(expectedTotal);
        assertThat(stone1.getTotalPrice()).isEqualByComparingTo(EXPECTED_STONE1_TOTAL);
        assertThat(stone2.getTotalPrice()).isEqualByComparingTo(EXPECTED_STONE2_TOTAL);
    }

    @Test
    @DisplayName("Calculate total stone cost with empty list - should return zero")
    void calculateTotalStoneCost_withEmptyList_returnsZero() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        jtrc.setStoneComponents(new HashSet<>());

        // When
        BigDecimal totalStoneCost = calculationService.calculateTotalStoneCost(jtrc);

        // Then
        assertThat(totalStoneCost).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate total stone cost with null list - should return zero")
    void calculateTotalStoneCost_withNullList_returnsZero() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        jtrc.setStoneComponents(null);

        // When
        BigDecimal totalStoneCost = calculationService.calculateTotalStoneCost(jtrc);

        // Then
        assertThat(totalStoneCost).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate total stone cost with null JTRC - should return zero")
    void calculateTotalStoneCost_withNullJTRC_returnsZero() {
        // When
        BigDecimal totalStoneCost = calculationService.calculateTotalStoneCost(null);

        // Then
        assertThat(totalStoneCost).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Total Labor Cost Tests ====================

    @Test
    @DisplayName("Calculate total labor cost with multiple labor components - should sum correctly")
    void calculateTotalLaborCost_withMultipleLabor_sumsCorrectly() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        JTRCLaborComponent labor1 = createLaborComponent(JTRCLaborType.SETTING, LABOR1_COST);
        JTRCLaborComponent labor2 = createLaborComponent(JTRCLaborType.FINISHING, LABOR2_COST);
        jtrc.addLaborComponent(labor1);
        jtrc.addLaborComponent(labor2);

        // When
        BigDecimal totalLaborCost = calculationService.calculateTotalLaborCost(jtrc);

        // Then
        assertThat(totalLaborCost).isEqualByComparingTo(EXPECTED_TOTAL_LABOR_COST);
    }

    @Test
    @DisplayName("Calculate total labor cost with null costs - should handle gracefully")
    void calculateTotalLaborCost_withNullCosts_handlesGracefully() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        JTRCLaborComponent labor1 = createLaborComponent(JTRCLaborType.SETTING, LABOR1_COST);
        JTRCLaborComponent labor2 = createLaborComponent(JTRCLaborType.FINISHING, null);
        jtrc.addLaborComponent(labor1);
        jtrc.addLaborComponent(labor2);

        // When
        BigDecimal totalLaborCost = calculationService.calculateTotalLaborCost(jtrc);

        // Then
        assertThat(totalLaborCost).isEqualByComparingTo(LABOR1_COST);
    }

    @Test
    @DisplayName("Calculate total labor cost with empty list - should return zero")
    void calculateTotalLaborCost_withEmptyList_returnsZero() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        jtrc.setLaborComponents(new HashSet<>());

        // When
        BigDecimal totalLaborCost = calculationService.calculateTotalLaborCost(jtrc);

        // Then
        assertThat(totalLaborCost).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate total labor cost with null JTRC - should return zero")
    void calculateTotalLaborCost_withNullJTRC_returnsZero() {
        // When
        BigDecimal totalLaborCost = calculationService.calculateTotalLaborCost(null);

        // Then
        assertThat(totalLaborCost).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Total COGS Tests ====================

    @Test
    @DisplayName("Calculate total COGS with all components - should sum correctly")
    void calculateTotalCOGS_withAllComponents_sumsCorrectly() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();

        // When
        BigDecimal totalCOGS = calculationService.calculateTotalCOGS(jtrc);

        // Then
        assertThat(totalCOGS).isEqualByComparingTo(EXPECTED_TOTAL_COGS_VND);
    }

    @Test
    @DisplayName("Calculate total COGS with missing components - should handle gracefully")
    void calculateTotalCOGS_withMissingComponents_handlesGracefully() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        // Only add metal component
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            METAL_PRICE_PER_GRAM,
            METAL_LOSS_RATE
        );
        jtrc.setMetalComponent(metalComponent);

        // When
        BigDecimal totalCOGS = calculationService.calculateTotalCOGS(jtrc);

        // Then
        assertThat(totalCOGS).isEqualByComparingTo(EXPECTED_TOTAL_METAL_COST);
    }

    @Test
    @DisplayName("Calculate total COGS with null JTRC - should return zero")
    void calculateTotalCOGS_withNullJTRC_returnsZero() {
        // When
        BigDecimal totalCOGS = calculationService.calculateTotalCOGS(null);

        // Then
        assertThat(totalCOGS).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculate total COGS with no components - should return zero")
    void calculateTotalCOGS_withNoComponents_returnsZero() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();

        // When
        BigDecimal totalCOGS = calculationService.calculateTotalCOGS(jtrc);

        // Then
        assertThat(totalCOGS).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== USD Conversion Tests ====================

    @Test
    @DisplayName("Convert to USD with valid rate - should convert correctly")
    void convertToUSD_withValidRate_convertsCorrectly() {
        // Given
        BigDecimal amountVnd = EXPECTED_TOTAL_COGS_VND;

        // When
        BigDecimal amountUsd = calculationService.convertToUSD(amountVnd, EXCHANGE_RATE);

        // Then
        assertThat(amountUsd).isEqualByComparingTo(EXPECTED_TOTAL_COGS_USD);
    }

    @Test
    @DisplayName("Convert to USD with zero rate - should return zero")
    void convertToUSD_withZeroRate_returnsZero() {
        // Given
        BigDecimal amountVnd = EXPECTED_TOTAL_COGS_VND;

        // When
        BigDecimal amountUsd = calculationService.convertToUSD(amountVnd, BigDecimal.ZERO);

        // Then
        assertThat(amountUsd).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Convert to USD with null rate - should return zero")
    void convertToUSD_withNullRate_returnsZero() {
        // Given
        BigDecimal amountVnd = EXPECTED_TOTAL_COGS_VND;

        // When
        BigDecimal amountUsd = calculationService.convertToUSD(amountVnd, null);

        // Then
        assertThat(amountUsd).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Convert to USD with null amount - should return zero")
    void convertToUSD_withNullAmount_returnsZero() {
        // When
        BigDecimal amountUsd = calculationService.convertToUSD(null, EXCHANGE_RATE);

        // Then
        assertThat(amountUsd).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Recalculate All Costs Tests ====================

    @Test
    @DisplayName("Recalculate all costs - should update all fields")
    void recalculateAllCosts_updatesAllFields() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();
        jtrc.setExchangeRateUsd(EXCHANGE_RATE);

        // When
        calculationService.recalculateAllCosts(jtrc);

        // Then
        assertThat(jtrc.getTotalMetalCost()).isEqualByComparingTo(EXPECTED_TOTAL_METAL_COST);
        assertThat(jtrc.getTotalStoneCost()).isEqualByComparingTo(
            EXPECTED_STONE1_TOTAL.add(EXPECTED_STONE2_TOTAL)
        );
        assertThat(jtrc.getTotalLaborCost()).isEqualByComparingTo(EXPECTED_TOTAL_LABOR_COST);
        assertThat(jtrc.getTotalCogsVnd()).isEqualByComparingTo(EXPECTED_TOTAL_COGS_VND);
        assertThat(jtrc.getTotalCogsUsd()).isEqualByComparingTo(EXPECTED_TOTAL_COGS_USD);

        // Verify component-level calculations
        assertThat(jtrc.getMetalComponent().getMetalCost()).isEqualByComparingTo(EXPECTED_METAL_COST);
        assertThat(jtrc.getMetalComponent().getLossCost()).isEqualByComparingTo(EXPECTED_LOSS_COST);
        assertThat(jtrc.getMetalComponent().getTotalCost()).isEqualByComparingTo(EXPECTED_TOTAL_METAL_COST);
    }

    @Test
    @DisplayName("Recalculate all costs with null JTRC - should not throw")
    void recalculateAllCosts_withNullJtrc_doesNotThrow() {
        // When/Then - should not throw exception
        calculationService.recalculateAllCosts(null);
    }

    @Test
    @DisplayName("Recalculate all costs without exchange rate - should set USD to zero")
    void recalculateAllCosts_withoutExchangeRate_setsUsdToZero() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();
        jtrc.setExchangeRateUsd(null);

        // When
        calculationService.recalculateAllCosts(jtrc);

        // Then
        assertThat(jtrc.getTotalCogsVnd()).isEqualByComparingTo(EXPECTED_TOTAL_COGS_VND);
        assertThat(jtrc.getTotalCogsUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Recalculate all costs with zero exchange rate - should set USD to zero")
    void recalculateAllCosts_withZeroExchangeRate_setsUsdToZero() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();
        jtrc.setExchangeRateUsd(BigDecimal.ZERO);

        // When
        calculationService.recalculateAllCosts(jtrc);

        // Then
        assertThat(jtrc.getTotalCogsVnd()).isEqualByComparingTo(EXPECTED_TOTAL_COGS_VND);
        assertThat(jtrc.getTotalCogsUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Recalculate all costs with partial components - should calculate available components")
    void recalculateAllCosts_withPartialComponents_calculatesAvailable() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            METAL_PRICE_PER_GRAM,
            METAL_LOSS_RATE
        );
        jtrc.setMetalComponent(metalComponent);
        jtrc.setExchangeRateUsd(EXCHANGE_RATE);

        // When
        calculationService.recalculateAllCosts(jtrc);

        // Then
        assertThat(jtrc.getTotalMetalCost()).isEqualByComparingTo(EXPECTED_TOTAL_METAL_COST);
        assertThat(jtrc.getTotalStoneCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jtrc.getTotalLaborCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jtrc.getTotalCogsVnd()).isEqualByComparingTo(EXPECTED_TOTAL_METAL_COST);
    }

    // ==================== Validate Costs Tests ====================

    @Test
    @DisplayName("Validate costs when valid - should return true")
    void validateCosts_whenValid_returnsTrue() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();
        calculationService.recalculateAllCosts(jtrc);

        // When
        boolean isValid = calculationService.validateCosts(jtrc);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Validate costs when mismatch - should return false")
    void validateCosts_whenMismatch_returnsFalse() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();
        calculationService.recalculateAllCosts(jtrc);

        // Manually set incorrect total to create mismatch
        jtrc.setTotalCogsVnd(new BigDecimal("1000000.00"));

        // When
        boolean isValid = calculationService.validateCosts(jtrc);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate costs with null JTRC - should return false")
    void validateCosts_withNullJTRC_returnsFalse() {
        // When
        boolean isValid = calculationService.validateCosts(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate costs with null total COGS - should handle gracefully")
    void validateCosts_withNullTotalCOGS_handlesGracefully() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();
        calculationService.recalculateAllCosts(jtrc);
        jtrc.setTotalCogsVnd(null);

        // When
        boolean isValid = calculationService.validateCosts(jtrc);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate costs with all null components - should return true for zero")
    void validateCosts_withAllNullComponents_returnsTrueForZero() {
        // Given
        JewelryTechnicalReport jtrc = createJTRC();
        jtrc.setTotalMetalCost(null);
        jtrc.setTotalStoneCost(null);
        jtrc.setTotalLaborCost(null);
        jtrc.setTotalCogsVnd(BigDecimal.ZERO);

        // When
        boolean isValid = calculationService.validateCosts(jtrc);

        // Then
        assertThat(isValid).isTrue();
    }

    // ==================== BigDecimal Precision Tests ====================

    @Test
    @DisplayName("Metal cost calculation - should maintain scale of 2 with HALF_UP rounding")
    void metalCost_shouldMaintainCorrectScale() {
        // Given
        JTRCMetalComponent metalComponent = createMetalComponent(
            new BigDecimal("5.555"),
            new BigDecimal("1850000.555"),
            new BigDecimal("3.555")
        );

        // When
        calculationService.calculateMetalCost(metalComponent);

        // Then
        assertThat(metalComponent.getMetalCost().scale()).isEqualTo(2);
        assertThat(metalComponent.getLossCost().scale()).isEqualTo(2);
        assertThat(metalComponent.getTotalCost().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Stone cost calculation - should maintain scale of 2 with HALF_UP rounding")
    void stoneCost_shouldMaintainCorrectScale() {
        // Given
        JTRCStoneComponent stoneComponent = createStoneComponent(
            3,
            new BigDecimal("333333.333")
        );

        // When
        calculationService.calculateStoneCost(stoneComponent);

        // Then
        assertThat(stoneComponent.getTotalPrice().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Total COGS calculation - should maintain scale of 2 with HALF_UP rounding")
    void totalCOGS_shouldMaintainCorrectScale() {
        // Given
        JewelryTechnicalReport jtrc = createFullJTRC();

        // When
        BigDecimal totalCOGS = calculationService.calculateTotalCOGS(jtrc);

        // Then
        assertThat(totalCOGS.scale()).isEqualTo(2);
    }

    // ==================== Helper Methods ====================

    /**
     * Create a basic JTRC entity for testing
     */
    private JewelryTechnicalReport createJTRC() {
        return JewelryTechnicalReport.builder()
            .reportNumber("JTRC-2026-TEST-001")
            .collection("Test Collection")
            .season("SS26")
            .category("Ring")
            .status(JTRCStatus.DRAFT)
            .stoneComponents(new HashSet<>())
            .laborComponents(new HashSet<>())
            .build();
    }

    /**
     * Create a fully populated JTRC with all components for testing
     */
    private JewelryTechnicalReport createFullJTRC() {
        JewelryTechnicalReport jtrc = createJTRC();

        // Add metal component
        JTRCMetalComponent metalComponent = createMetalComponent(
            METAL_WEIGHT,
            METAL_PRICE_PER_GRAM,
            METAL_LOSS_RATE
        );
        jtrc.setMetalComponent(metalComponent);

        // Add stone components
        JTRCStoneComponent stone1 = createStoneComponent(STONE1_QUANTITY, STONE1_UNIT_PRICE);
        JTRCStoneComponent stone2 = createStoneComponent(STONE2_QUANTITY, STONE2_UNIT_PRICE);
        jtrc.addStoneComponent(stone1);
        jtrc.addStoneComponent(stone2);

        // Add labor components
        JTRCLaborComponent labor1 = createLaborComponent(JTRCLaborType.SETTING, LABOR1_COST);
        JTRCLaborComponent labor2 = createLaborComponent(JTRCLaborType.FINISHING, LABOR2_COST);
        jtrc.addLaborComponent(labor1);
        jtrc.addLaborComponent(labor2);

        return jtrc;
    }

    /**
     * Create a metal component for testing
     */
    private JTRCMetalComponent createMetalComponent(
        BigDecimal weight,
        BigDecimal pricePerGram,
        BigDecimal lossRate
    ) {
        return JTRCMetalComponent.builder()
            .metalType("Yellow Gold")
            .metalPurity("18K")
            .weightGrams(weight)
            .pricePerGram(pricePerGram)
            .lossRatePercent(lossRate)
            .build();
    }

    /**
     * Create a stone component for testing
     */
    private JTRCStoneComponent createStoneComponent(Integer quantity, BigDecimal unitPrice) {
        return JTRCStoneComponent.builder()
            .stoneRole(StoneRole.MAIN)
            .stoneType("Lab Diamond")
            .shape("Round Brilliant")
            .colorCategory(StoneColorCategory.COLORLESS)
            .colorGrade("D")
            .clarity("VVS1")
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
    }

    /**
     * Create a labor component for testing
     */
    private JTRCLaborComponent createLaborComponent(JTRCLaborType laborType, BigDecimal cost) {
        return JTRCLaborComponent.builder()
            .laborType(laborType)
            .description(laborType.name())
            .cost(cost)
            .build();
    }
}
