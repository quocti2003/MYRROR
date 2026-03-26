package com.mirror.product.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utility for verifying MISA callback signatures using SHA256HMAC
 *
 * Per MISA documentation, the signature is computed using:
 * - Algorithm: SHA256HMAC
 * - Key: app_id provided by MISA
 * - Data: The JSON payload string
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MisaSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;

    /**
     * Verify if the provided signature matches the computed signature
     *
     * @param payload The raw JSON payload string
     * @param signature The signature from the request
     * @param appId The app_id used as the HMAC key
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String payload, String signature, String appId) {
        if (payload == null || signature == null || appId == null) {
            log.warn("Cannot verify signature: missing required parameters");
            return false;
        }

        try {
            String computedSignature = computeSignature(payload, appId);

            // Try comparing with different encodings
            boolean isValid = signature.equalsIgnoreCase(computedSignature);

            if (!isValid) {
                // Try Base64 comparison
                String base64Signature = computeSignatureBase64(payload, appId);
                isValid = signature.equals(base64Signature);
            }

            if (!isValid) {
                log.debug("Signature mismatch. Expected: {}, Received: {}", computedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    /**
     * Compute HMAC-SHA256 signature (hex encoded)
     *
     * @param data The data to sign
     * @param key The secret key
     * @return Hex-encoded signature
     */
    public String computeSignature(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmacBytes);
    }

    /**
     * Compute HMAC-SHA256 signature (Base64 encoded)
     *
     * @param data The data to sign
     * @param key The secret key
     * @return Base64-encoded signature
     */
    public String computeSignatureBase64(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    /**
     * Verify signature with the data object (will be serialized to JSON)
     *
     * @param dataObject The data object to verify
     * @param signature The signature from the request
     * @param appId The app_id used as the HMAC key
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(Object dataObject, String signature, String appId) {
        try {
            String payload = objectMapper.writeValueAsString(dataObject);
            return verifySignature(payload, signature, appId);
        } catch (Exception e) {
            log.error("Error serializing data object for signature verification", e);
            return false;
        }
    }

    /**
     * Generate a signature for outgoing requests
     *
     * @param data The data to sign
     * @param key The secret key
     * @return Hex-encoded signature
     */
    public String generateSignature(String data, String key) {
        try {
            return computeSignature(data, key);
        } catch (Exception e) {
            log.error("Error generating signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
