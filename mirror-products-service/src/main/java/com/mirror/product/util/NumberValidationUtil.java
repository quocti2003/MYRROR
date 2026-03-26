package com.mirror.product.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for number validation and rounding
 */
public class NumberValidationUtil {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([0-9]+\\.?[0-9]*)([A-Z]*)");
    private static final BigDecimal MIN_VALUE = BigDecimal.ZERO;
    private static final BigDecimal MAX_VALUE = new BigDecimal("100000");

    /**
     * Validates and processes a numeric string with optional unit suffix (e.g., "3.5G", "5.234G")
     * - Extracts the numeric part
     * - Validates it's > 0 and < 100000
     * - Rounds to 2 decimal places
     * - Appends the unit suffix back
     *
     * @param value The input value (e.g., "3.5G", "5.234G")
     * @return Processed value with rounded number and unit (e.g., "3.50G", "5.23G")
     * @throws IllegalArgumentException if validation fails
     */
    public static String validateAndRoundWeight(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value; // Allow empty/null values for optional fields
        }

        String trimmed = value.trim().toUpperCase();
        Matcher matcher = NUMERIC_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid weight format: " + value + ". Expected format: number with optional unit (e.g., 3.5G)");
        }

        String numericPart = matcher.group(1);
        String unit = matcher.group(2);

        try {
            BigDecimal number = new BigDecimal(numericPart);

            // Validate range
            if (number.compareTo(MIN_VALUE) <= 0) {
                throw new IllegalArgumentException("Weight must be greater than 0. Got: " + value);
            }
            if (number.compareTo(MAX_VALUE) >= 0) {
                throw new IllegalArgumentException("Weight must be less than 100000. Got: " + value);
            }

            // Round to 2 decimal places
            BigDecimal rounded = number.setScale(2, RoundingMode.HALF_UP);

            // Return formatted value with unit
            return rounded.toPlainString() + unit;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in weight: " + value, e);
        }
    }

    /**
     * Validates a plain numeric value (without unit)
     * - Validates it's > 0 and < 100000
     * - Rounds to 2 decimal places
     *
     * @param value The numeric value as string
     * @return Rounded value as string
     * @throws IllegalArgumentException if validation fails
     */
    public static String validateAndRoundNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        try {
            BigDecimal number = new BigDecimal(value.trim());

            // Validate range
            if (number.compareTo(MIN_VALUE) <= 0) {
                throw new IllegalArgumentException("Number must be greater than 0. Got: " + value);
            }
            if (number.compareTo(MAX_VALUE) >= 0) {
                throw new IllegalArgumentException("Number must be less than 100000. Got: " + value);
            }

            // Round to 2 decimal places
            BigDecimal rounded = number.setScale(2, RoundingMode.HALF_UP);

            return rounded.toPlainString();

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + value, e);
        }
    }
}
