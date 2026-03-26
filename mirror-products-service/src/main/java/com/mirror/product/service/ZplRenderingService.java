package com.mirror.product.service;

import com.mirror.product.entity.LabelTemplate;
import com.mirror.product.entity.MirrorProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for rendering ZPL templates with variable substitution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZplRenderingService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Z_0-9]+)\\}\\}");

    /**
     * Render a ZPL template by substituting variables with values
     *
     * @param template The label template containing ZPL with placeholders
     * @param variables Map of variable names to values
     * @return Rendered ZPL string ready for printing
     */
    public String renderZpl(LabelTemplate template, Map<String, String> variables) {
        if (template == null || template.getZplContent() == null) {
            throw new IllegalArgumentException("Template and ZPL content are required");
        }

        String zpl = template.getZplContent();

        if (variables == null || variables.isEmpty()) {
            return zpl;
        }

        // Replace all {{VARIABLE}} placeholders
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(zpl);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables.getOrDefault(variableName, "");
            // Escape special characters in ZPL
            value = escapeZplValue(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Render a ZPL template using product data to populate variables
     *
     * @param template The label template
     * @param product The product to get data from
     * @param additionalVariables Additional variables to merge (override product data)
     * @return Rendered ZPL string
     */
    public String renderZplForProduct(LabelTemplate template, MirrorProduct product, Map<String, String> additionalVariables) {
        Map<String, String> variables = extractProductVariables(product);

        // Merge additional variables (overrides product data)
        if (additionalVariables != null) {
            variables.putAll(additionalVariables);
        }

        return renderZpl(template, variables);
    }

    /**
     * Extract standard variables from a MirrorProduct entity
     *
     * @param product The product entity
     * @return Map of variable names to values
     */
    public Map<String, String> extractProductVariables(MirrorProduct product) {
        Map<String, String> variables = new HashMap<>();

        if (product == null) {
            return variables;
        }

        // Basic product info
        variables.put("PRODUCT_ID", safeString(product.getId()));
        variables.put("SKU", safeString(product.getSkuCode()));
        variables.put("DESCRIPTIVE_CODE", safeString(product.getDescriptiveCode()));
        variables.put("BARCODE", safeString(product.getBarcode()));
        variables.put("NAME", safeString(product.getItemName()));
        variables.put("PRODUCT_NAME", safeString(product.getItemName()));
        variables.put("DESCRIPTION", truncate(safeString(product.getDescription()), 50));

        // Category
        variables.put("CATEGORY", safeString(product.getCategory()));

        // Pricing
        if (product.getPrice() != null) {
            variables.put("PRICE", formatPrice(product.getPrice()));
            variables.put("PRICE_RAW", product.getPrice().toPlainString());
        } else {
            variables.put("PRICE", "");
            variables.put("PRICE_RAW", "");
        }

        if (product.getCost() != null) {
            variables.put("COST", formatPrice(product.getCost()));
        } else {
            variables.put("COST", "");
        }

        // Jewelry specifications
        variables.put("METAL_TYPE", safeString(product.getMetalType()));
        variables.put("METAL_PURITY", safeString(product.getMetalPurity()));
        variables.put("STONE_TYPE", safeString(product.getStoneType()));
        variables.put("STONE_SHAPE", safeString(product.getStoneShape()));

        if (product.getWeightGrams() != null) {
            variables.put("WEIGHT", String.format("%.2f", product.getWeightGrams()) + "g");
            variables.put("WEIGHT_GRAMS", String.format("%.2f", product.getWeightGrams()));
        } else {
            variables.put("WEIGHT", "");
            variables.put("WEIGHT_GRAMS", "");
        }

        // stoneWeight is stored as String (e.g., "1.5ct")
        variables.put("STONE_WEIGHT", safeString(product.getStoneWeight()));
        variables.put("STONE_CARAT", safeString(product.getStoneWeight()));

        // Material color
        variables.put("COLOR", safeString(product.getMaterialColor()));
        variables.put("MATERIAL_COLOR", safeString(product.getMaterialColor()));

        // Origin
        variables.put("COUNTRY_OF_ORIGIN", safeString(product.getCountryOfOrigin()));
        variables.put("STONE_ORIGIN", safeString(product.getStoneOrigin()));

        // Dimensions
        variables.put("DIMENSIONS", safeString(product.getDimensions()));

        // Status
        variables.put("STATUS", safeString(product.getStatus() != null ? product.getStatus().name() : ""));

        // Date
        variables.put("DATE", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        variables.put("YEAR", String.valueOf(LocalDate.now().getYear()));

        // Stock info
        if (product.getStockQuantity() != null) {
            variables.put("STOCK", String.valueOf(product.getStockQuantity()));
        } else {
            variables.put("STOCK", "0");
        }

        return variables;
    }

    /**
     * Get a list of all supported variable names
     */
    public String[] getSupportedVariables() {
        return new String[] {
            "PRODUCT_ID", "SKU", "DESCRIPTIVE_CODE", "BARCODE", "NAME", "PRODUCT_NAME", "DESCRIPTION",
            "CATEGORY", "PRICE", "PRICE_RAW", "COST",
            "METAL_TYPE", "METAL_PURITY", "STONE_TYPE", "STONE_SHAPE",
            "WEIGHT", "WEIGHT_GRAMS", "STONE_WEIGHT", "STONE_CARAT",
            "COLOR", "MATERIAL_COLOR", "COUNTRY_OF_ORIGIN", "STONE_ORIGIN",
            "DIMENSIONS", "STATUS", "DATE", "YEAR", "STOCK"
        };
    }

    // ==================== Private Helper Methods ====================

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String formatPrice(BigDecimal price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(price);
    }

    private String escapeZplValue(String value) {
        if (value == null) {
            return "";
        }
        // ZPL uses backslash as escape character
        // Escape backslashes and other special characters if needed
        return value
                .replace("\\", "\\\\")
                .replace("^", "\\^")
                .replace("~", "\\~");
    }
}
