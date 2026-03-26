package com.mirror.product.service;

import com.mirror.product.dto.sku.SkuGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SkuCodeService {

    // No length limit - SKU codes can be as long as needed to capture all product info
    // Barcodes are separate (MIR{timestamp}{random} format, 17 chars)
    private static final Pattern ALPHANUMERIC_DOT = Pattern.compile("[^A-Z0-9.]");

    // Abbreviation dictionary (ported from Python app.py)
    // Order matters: longer strings must be processed before shorter ones
    private static final Map<String, String> ABBREVIATIONS = new LinkedHashMap<>();
    static {
        // Gold types (process longer ones first)
        ABBREVIATIONS.put("18KWHITEGOLD", "18KWG");
        ABBREVIATIONS.put("18KYELLOWGOLD", "18KYG");
        ABBREVIATIONS.put("18KROSEGOLD", "18KRG");
        ABBREVIATIONS.put("14KWHITEGOLD", "14KWG");
        ABBREVIATIONS.put("14KYELLOWGOLD", "14KYG");
        ABBREVIATIONS.put("WHITEGOLD", "WG");
        ABBREVIATIONS.put("YELLOWGOLD", "YG");
        ABBREVIATIONS.put("ROSEGOLD", "RG");

        // Diamond origins
        ABBREVIATIONS.put("LABGROWN", "LG");
        ABBREVIATIONS.put("NATURAL", "NAT");

        // Material colors (standalone - abbreviate to single char)
        ABBREVIATIONS.put("WHITE", "W");
        ABBREVIATIONS.put("YELLOW", "Y");
        ABBREVIATIONS.put("ROSE", "R");
        ABBREVIATIONS.put("PINK", "P");

        // Diamond shapes (abbreviate for space)
        ABBREVIATIONS.put("PRINCESS", "PRI");
        ABBREVIATIONS.put("CUSHION", "CU");
        ABBREVIATIONS.put("EMERALD", "EM");
        ABBREVIATIONS.put("ROUND", "RD");
        ABBREVIATIONS.put("OVAL", "OV");
        ABBREVIATIONS.put("PEAR", "PR");
        ABBREVIATIONS.put("MARQUISE", "MQ");
        ABBREVIATIONS.put("RADIANT", "RAD");
        ABBREVIATIONS.put("ASSCHER", "AS");
        ABBREVIATIONS.put("HEART", "HT");

        // Side stones
        ABBREVIATIONS.put("DIAMONDS", "DIA");
        ABBREVIATIONS.put("NONE", "N");

        // Packaging materials
        ABBREVIATIONS.put("MICROFIBER", "MF");
        ABBREVIATIONS.put("LEATHERETTE", "LTHRTTE");
        ABBREVIATIONS.put("VELVET", "VLV");
        ABBREVIATIONS.put("BRIGHTSILVER", "BRSLV");
        ABBREVIATIONS.put("CHAMPAGNE", "CHMP");
        ABBREVIATIONS.put("DEBOSSCENTER", "DBCTR");
        ABBREVIATIONS.put("EDGESTHINNERTHANSAMPLE", "EDGTHN");
        ABBREVIATIONS.put("SILVERCENTERONLIP", "SLVCTR");
    }

    /**
     * Generate jewelry code
     * Format: PREFIX-MATERIAL-MATERIALCOLOR-MATERIALWEIGHT-[COATING]-ORIGIN-SHAPE-WEIGHT-SIDE-VARIANT
     */
    public SkuGenerationResult generateJewelrySku(
            String prefix,
            String material,
            String materialColor,
            String materialWeight,
            Boolean isCoated,
            String coatingMaterial,
            String origin,
            String shape,
            String weight,
            String sideStones,
            String variant) {

        // Build coating segment if applicable
        String coatingSegment = (isCoated != null && isCoated && coatingMaterial != null && !coatingMaterial.isBlank())
            ? coatingMaterial + "COATED"
            : null;

        return buildCode(
            prefix,
            material,
            materialColor,
            materialWeight,
            coatingSegment,
            origin,
            shape,
            weight,
            sideStones,
            variant
        );
    }

    /**
     * Generate packaging code
     * Format: PREFIX-MATERIAL-SIZE-COLOR-TYPE-FINISH-NOTES
     */
    public SkuGenerationResult generatePackagingSku(
            String prefix,
            String material,
            String size,
            String color,
            String type,
            String finish,
            String notes) {

        return buildCode(prefix, material, size, color, type, finish, notes);
    }

    /**
     * Sanitize token (ported from Python)
     */
    private String sanitizeToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }

        // Convert to uppercase, remove spaces
        String cleaned = token.strip().toUpperCase();
        cleaned = cleaned.replace(" ", "");

        // Remove non-alphanumeric (keep dots)
        cleaned = ALPHANUMERIC_DOT.matcher(cleaned).replaceAll("");

        return cleaned;
    }

    /**
     * Apply abbreviations to token
     */
    private String abbreviateCommonTerms(String token) {
        String result = token;

        // Apply abbreviations
        for (Map.Entry<String, String> entry : ABBREVIATIONS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Build code by joining all non-empty parts with hyphens.
     * No length limit - all product information is preserved.
     * Abbreviations are applied to keep codes reasonably compact.
     */
    private SkuGenerationResult buildCode(String... parts) {
        // Filter and sanitize parts
        List<String> tokens = Arrays.stream(parts)
            .filter(part -> part != null && !part.isBlank())
            .map(this::sanitizeToken)
            .filter(token -> !token.isEmpty())
            .map(this::abbreviateCommonTerms)
            .collect(Collectors.toCollection(ArrayList::new));

        if (tokens.isEmpty()) {
            return new SkuGenerationResult("", false);
        }

        // Join with hyphens - no truncation
        String code = String.join("-", tokens);

        log.debug("Generated code: {} (length: {})", code, code.length());
        return new SkuGenerationResult(code, false);
    }

    /**
     * Extract material from product name
     */
    public String extractMaterialFromDescription(String name) {
        String nameLower = name.toLowerCase();
        if (nameLower.contains("18k white gold") || nameLower.contains("18kwg")) {
            return "18KWG";
        } else if (nameLower.contains("18k yellow gold") || nameLower.contains("18kyg")) {
            return "18KYG";
        } else if (nameLower.contains("18k rose gold") || nameLower.contains("18krg")) {
            return "18KRG";
        } else if (nameLower.contains("gold")) {
            return "GOLD";
        } else if (nameLower.contains("silver")) {
            return "SILVER";
        } else if (nameLower.contains("microfiber")) {
            return "MF";
        }
        return "";
    }

    /**
     * Extract origin from product name
     */
    public String extractOriginFromDescription(String name) {
        String nameLower = name.toLowerCase();
        if (nameLower.contains("lab grown") || nameLower.contains("labgrown") || nameLower.contains("lab-grown")) {
            return "LG";
        } else if (nameLower.contains("natural")) {
            return "NAT";
        }
        return "";
    }

    /**
     * Extract shape from product name
     */
    private String extractShape(String name) {
        String nameLower = name.toLowerCase();
        if (nameLower.contains("pear")) {
            return "PEAR";
        } else if (nameLower.contains("round")) {
            return "ROUND";
        } else if (nameLower.contains("emerald")) {
            return "EMERALD";
        } else if (nameLower.contains("oval")) {
            return "OVAL";
        } else if (nameLower.contains("cushion")) {
            return "CUSHION";
        } else if (nameLower.contains("princess")) {
            return "PRINCESS";
        }
        return "";
    }
}
