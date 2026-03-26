package com.mirror.product.service;

import com.mirror.product.repository.MirrorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Service for generating unique EAN-13 barcodes for products.
 * Generates valid 13-digit EAN-13 barcodes with proper check digit.
 * Format: {12-digit-data}{1-digit-checksum}
 * Example: 7676950963035
 *
 * The check digit is calculated according to EAN-13 standard to ensure
 * barcode scanners and printers handle the barcode correctly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BarcodeGenerationService {

    private static final int MAX_GENERATION_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BARCODE_LENGTH = 13;
    private static final int DATA_LENGTH = 12; // 12 data digits + 1 check digit = 13

    private final MirrorProductRepository mirrorProductRepository;

    /**
     * Generates a unique EAN-13 barcode that doesn't exist in the database.
     * The barcode includes a valid check digit calculated per EAN-13 standard.
     *
     * @return A unique EAN-13 barcode string (13 digits with valid check digit)
     * @throws IllegalStateException if unable to generate a unique barcode after max attempts
     */
    @Transactional(readOnly = true)
    public String generateUniqueBarcode() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String barcode = generateBarcode();

            if (!barcodeExists(barcode)) {
                log.debug("Generated unique EAN-13 barcode: {} (attempt {})", barcode, attempt);
                return barcode;
            }

            log.debug("Barcode collision detected: {} (attempt {}), retrying...", barcode, attempt);
        }

        throw new IllegalStateException(
            "Failed to generate unique barcode after " + MAX_GENERATION_ATTEMPTS + " attempts"
        );
    }

    /**
     * Generates a valid EAN-13 barcode string without checking for uniqueness.
     * Format: {8-digit-timestamp}{4-random-digits}{1-check-digit} = 13 digits
     * The check digit is calculated according to EAN-13 standard.
     */
    private String generateBarcode() {
        long timestamp = Instant.now().getEpochSecond();
        // Use last 8 digits of timestamp
        long timestampSuffix = timestamp % 100_000_000L;
        int randomSuffix = RANDOM.nextInt(10000); // 0-9999

        // Generate 12 data digits
        String dataDigits = String.format("%08d%04d", timestampSuffix, randomSuffix);

        // Calculate and append check digit
        int checkDigit = calculateEan13CheckDigit(dataDigits);

        return dataDigits + checkDigit;
    }

    /**
     * Calculates the EAN-13 check digit for a 12-digit string.
     * Algorithm:
     * 1. Sum digits at odd positions (1,3,5,7,9,11)
     * 2. Sum digits at even positions (2,4,6,8,10,12) and multiply by 3
     * 3. Add both sums
     * 4. Check digit = (10 - (total mod 10)) mod 10
     *
     * @param dataDigits 12-digit string
     * @return The check digit (0-9)
     */
    public int calculateEan13CheckDigit(String dataDigits) {
        if (dataDigits == null || dataDigits.length() != DATA_LENGTH) {
            throw new IllegalArgumentException("Data must be exactly 12 digits");
        }

        int oddSum = 0;
        int evenSum = 0;

        for (int i = 0; i < DATA_LENGTH; i++) {
            int digit = Character.getNumericValue(dataDigits.charAt(i));
            if (i % 2 == 0) {
                // Positions 1,3,5,7,9,11 (0-indexed: 0,2,4,6,8,10)
                oddSum += digit;
            } else {
                // Positions 2,4,6,8,10,12 (0-indexed: 1,3,5,7,9,11)
                evenSum += digit;
            }
        }

        int total = oddSum + (evenSum * 3);
        return (10 - (total % 10)) % 10;
    }

    /**
     * Checks if a barcode already exists in the database.
     */
    private boolean barcodeExists(String barcode) {
        return mirrorProductRepository.findByBarcode(barcode).isPresent();
    }

    /**
     * Validates that a barcode matches EAN-13 format with correct check digit.
     *
     * @param barcode The barcode to validate
     * @return true if the barcode format is valid and check digit is correct
     */
    public boolean isValidBarcodeFormat(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return false;
        }

        // Expected format: 13 digits only
        if (barcode.length() != BARCODE_LENGTH) {
            return false;
        }

        // Must be exactly 13 digits
        if (!barcode.matches("\\d{13}")) {
            return false;
        }

        // Verify check digit
        String dataDigits = barcode.substring(0, DATA_LENGTH);
        int expectedCheckDigit = calculateEan13CheckDigit(dataDigits);
        int actualCheckDigit = Character.getNumericValue(barcode.charAt(12));

        return expectedCheckDigit == actualCheckDigit;
    }

    /**
     * Corrects an invalid EAN-13 barcode by recalculating its check digit.
     * Useful for fixing existing barcodes with incorrect check digits.
     *
     * @param barcode The 13-digit barcode (possibly with wrong check digit)
     * @return The corrected barcode with valid check digit
     */
    public String correctBarcodeCheckDigit(String barcode) {
        if (barcode == null || barcode.length() != BARCODE_LENGTH) {
            throw new IllegalArgumentException("Barcode must be exactly 13 digits");
        }

        if (!barcode.matches("\\d{13}")) {
            throw new IllegalArgumentException("Barcode must contain only digits");
        }

        String dataDigits = barcode.substring(0, DATA_LENGTH);
        int checkDigit = calculateEan13CheckDigit(dataDigits);

        return dataDigits + checkDigit;
    }
}
