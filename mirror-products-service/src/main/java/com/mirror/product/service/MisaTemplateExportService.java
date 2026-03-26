package com.mirror.product.service;

import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.repository.MirrorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting products to MISA-compatible Excel template.
 *
 * New model (simplified):
 * - Each product is unique with its own barcode
 * - skuCode = barcode (unique identifier)
 * - descriptiveCode = human-readable product specs
 * - No more serialized units - each product is exported as one row
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MisaTemplateExportService {

    private static final String MISA_TEMPLATE_PATH = "templates/misa_inventory_template.xls";
    private static final int DATA_START_ROW_INDEX = 4; // Row index where data begins (zero-based)
    private static final int TOTAL_TEMPLATE_COLUMNS = 43;
    private static final String DEFAULT_BRAND_NAME = "Mirror Future Diamond";
    private static final String DEFAULT_YES = "Có";
    private static final String DEFAULT_NO = "Không";

    private final MirrorProductRepository mirrorProductRepository;

    /**
     * Export all generated SKUs to MISA import template format.
     * Each product is exported as a single row.
     */
    public byte[] exportToMisaTemplate() throws IOException {
        List<MirrorProduct> products = mirrorProductRepository.findAll();
        return generateMisaExcel(products);
    }

    /**
     * Export specific products by SKU codes to MISA template
     */
    public byte[] exportProductsByIds(List<String> skuCodes) throws IOException {
        List<MirrorProduct> products = mirrorProductRepository.findBySkuCodeIn(skuCodes);
        log.info("Found {} products for {} SKU codes", products.size(), skuCodes.size());
        return generateMisaExcel(products);
    }

    /**
     * Export products by category to MISA template
     */
    public byte[] exportProductsByCategory(String category) throws IOException {
        List<MirrorProduct> products = mirrorProductRepository.findByCategory(category);
        return generateMisaExcel(products);
    }

    /**
     * Generate MISA-format Excel file from products.
     * Each product is exported as a single row.
     */
    private byte[] generateMisaExcel(List<MirrorProduct> products) throws IOException {
        ClassPathResource templateResource = new ClassPathResource(MISA_TEMPLATE_PATH);

        if (!templateResource.exists()) {
            throw new IOException("MISA template not found on classpath: " + MISA_TEMPLATE_PATH);
        }

        try (InputStream templateStream = templateResource.getInputStream();
             Workbook workbook = WorkbookFactory.create(templateStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("MISA template does not contain any worksheets");
            }

            clearTemplateDataRows(sheet);

            CellStyle dataStyle = createDataStyle(workbook);

            int rowIndex = DATA_START_ROW_INDEX;

            for (MirrorProduct product : products) {
                Row row = getOrCreateRow(sheet, rowIndex);
                populateProductRow(row, product, dataStyle);
                rowIndex++;
            }

            workbook.write(outputStream);
            byte[] bytes = outputStream.toByteArray();
            log.info("Generated MISA template with {} rows ({} products), file size: {} bytes",
                     products.size(), products.size(), bytes.length);
            return bytes;

        } catch (Exception e) {
            log.error("Error generating MISA Excel file", e);
            throw new IOException("Failed to generate MISA template", e);
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        } else {
            clearRowCells(row);
        }
        return row;
    }

    /**
     * Populate a single product row with data.
     * Uses skuCode (barcode) as the unique identifier.
     */
    private void populateProductRow(Row row, MirrorProduct product, CellStyle style) {
        int column = 0;

        createCell(row, column++, product.getSkuCode(), style); // SKUCode (A) - barcode-based
        createCell(row, column++, product.getBarcode(), style); // Barcode (B)
        createCell(row, column++, product.getMisaItemCode(), style); // ModelCode (C)
        createCell(row, column++, deriveModelName(product), style); // ModelName (D)
        createCell(row, column++, deriveInventoryItemName(product), style); // InventoryItemName (E)
        createCell(row, column++, "", style); // ItemCategoryCode (F)
        createCell(row, column++, safeString(product.getCategory()), style); // ItemCategoryName (G)
        createCell(row, column++, deriveBrandName(product), style); // BrandName (H)
        createCell(row, column++, getDefaultUnit(product.getCategory()), style); // UnitName (I)
        createCell(row, column++, extractColor(product), style); // Color (J)
        createCell(row, column++, extractSize(product), style); // Size (K)
        createCell(row, column++, product.getCost(), style); // CostPrice (L)
        createCell(row, column++, product.getPrice(), style); // UnitPrice (M)
        createCell(row, column++, "KCT", style); // TaxRate (N)
        createCell(row, column++, product.getStockQuantity(), style); // OpeningQuantity (O)
        createCell(row, column++, calculateOpeningAmount(product), style); // OpeningAmount (P)
        createCell(row, column++, deriveOpeningStockName(product), style); // OpeningStockName (Q)
        createCell(row, column++, product.getMinStockLevel(), style); // MinimumStock (R)
        createCell(row, column++, "", style); // MaximumStock (S)
        createCell(row, column++, "", style); // UnitConvertName (T)
        createCell(row, column++, "", style); // UnitConvertRate (U)
        createCell(row, column++, "", style); // UnitConvertCostPrice (V)
        createCell(row, column++, "", style); // UnitConvertSalePrice (W)
        createCell(row, column++, DEFAULT_YES, style); // IsSaleUnit (X)
        createCell(row, column++, DEFAULT_YES, style); // IsCostUnit (Y)
        createCell(row, column++, "", style); // ImageUrl (Z)
        createCell(row, column++, "", style); // Height (AA)
        createCell(row, column++, "", style); // Width (AB)
        createCell(row, column++, "", style); // Length (AC)
        createCell(row, column++, extractWeight(product), style); // Weight (AD)
        String showLocation = deriveShowLocation(product);
        createCell(row, column++, showLocation, style); // ShowLocation (AE)
        createCell(row, column++, deriveStockLocation(product, showLocation), style); // StockLocation (AF)
        createCell(row, column++, "", style); // IsUseLotNo (AG)
        createCell(row, column++, "", style); // SellBeforeDay (AH)
        createCell(row, column++, "", style); // IsUseSerial (AI) - leave empty per MISA requirements
        createCell(row, column++, DEFAULT_NO, style); // ShowInMenu (AJ)
        createCell(row, column++, safeString(product.getDescription()), style); // Description (AK)
        createCell(row, column++, "", style); // Inactive (AL)
        createCell(row, column++, deriveSizeRange(product), style); // SizeRange (AM)
        createCell(row, column++, extractMaterial(product), style); // Ingredient (AN)
        createCell(row, column++, deriveYearOfProduction(product), style); // YearOfProduction (AO)
        createCell(row, column++, "", style); // UnitPriceBox (AP)
        createCell(row, column++, "", style); // UnitPriceWholeSale (AQ)
    }

    private void clearTemplateDataRows(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                clearRowCells(row);
            }
        }
    }

    private void clearRowCells(Row row) {
        for (int columnIndex = 0; columnIndex < TOTAL_TEMPLATE_COLUMNS; columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            if (cell != null) {
                row.removeCell(cell);
            }
        }
    }

    private String deriveModelName(MirrorProduct product) {
        // Use descriptive code if available for model name
        if (product.getDescriptiveCode() != null && !product.getDescriptiveCode().isBlank()) {
            return product.getDescriptiveCode();
        }
        if (product.getCategory() != null && !product.getCategory().isBlank()) {
            return product.getCategory();
        }
        return deriveInventoryItemName(product);
    }

    private String deriveInventoryItemName(MirrorProduct product) {
        String itemName = product.getItemName();
        if (itemName != null && !itemName.isBlank()) {
            return itemName;
        }
        return safeString(product.getSkuCode());
    }

    private String deriveBrandName(MirrorProduct product) {
        return DEFAULT_BRAND_NAME;
    }

    private BigDecimal calculateOpeningAmount(MirrorProduct product) {
        if (product.getPrice() == null || product.getStockQuantity() == null) {
            return null;
        }
        return product.getPrice().multiply(BigDecimal.valueOf(product.getStockQuantity()));
    }

    private String deriveOpeningStockName(MirrorProduct product) {
        return safeString(product.getLocation());
    }

    private String deriveShowLocation(MirrorProduct product) {
        return safeString(product.getLocation());
    }

    private String deriveStockLocation(MirrorProduct product, String showLocationFallback) {
        String location = safeString(product.getLocation());
        if (!location.isEmpty()) {
            return location;
        }
        return safeString(showLocationFallback);
    }

    private String deriveSizeRange(MirrorProduct product) {
        return extractSize(product);
    }

    private String deriveYearOfProduction(MirrorProduct product) {
        LocalDateTime createdAt = product.getCreatedAt();
        if (createdAt != null) {
            return String.valueOf(createdAt.getYear());
        }
        return String.valueOf(LocalDateTime.now().getYear());
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    /**
     * Create a cell with value and style
     */
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);

        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue(value.toString());
        }

        cell.setCellStyle(style);
    }

    /**
     * Create data cell style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setWrapText(true);
        return style;
    }

    /**
     * Get default unit based on category
     */
    private String getDefaultUnit(String category) {
        if (category == null) return "cái";

        String categoryLower = category.toLowerCase();
        if (categoryLower.contains("jewelry") || categoryLower.contains("ring") ||
            categoryLower.contains("earring") || categoryLower.contains("necklace") ||
            categoryLower.contains("bracelet")) {
            return "cái";
        }
        if (categoryLower.contains("packaging") || categoryLower.contains("box")) {
            return "cái";
        }
        return "cái"; // Default: piece
    }

    /**
     * Extract color information from product data
     */
    private String extractColor(MirrorProduct product) {
        // First check materialColor field
        if (product.getMaterialColor() != null && !product.getMaterialColor().isBlank()) {
            return product.getMaterialColor();
        }

        if (product.getDescription() == null) return "";

        String desc = product.getDescription().toLowerCase();
        if (desc.contains("white gold") || desc.contains("whitegold")) return "White Gold";
        if (desc.contains("yellow gold") || desc.contains("yellowgold")) return "Yellow Gold";
        if (desc.contains("rose gold") || desc.contains("rosegold")) return "Rose Gold";
        if (desc.contains("red")) return "Red";
        if (desc.contains("black")) return "Black";
        if (desc.contains("grey") || desc.contains("gray")) return "Grey";
        if (desc.contains("silver")) return "Silver";
        if (desc.contains("navy")) return "Navy";

        return "";
    }

    /**
     * Extract size information from product data
     */
    private String extractSize(MirrorProduct product) {
        if (product.getDescription() == null) return "";

        String desc = product.getDescription();
        // Try to extract size patterns like "60x60x60", "260x200x52", etc.
        if (desc.matches(".*\\d+x\\d+x\\d+.*")) {
            String[] parts = desc.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+x\\d+x\\d+.*")) {
                    return part.replaceAll("[^0-9x]", "");
                }
            }
        }

        return "";
    }

    /**
     * Extract weight information from product data
     */
    private String extractWeight(MirrorProduct product) {
        // First check weightGrams field
        if (product.getWeightGrams() != null) {
            return product.getWeightGrams().toString();
        }

        // Then check materialWeight field
        if (product.getMaterialWeight() != null && !product.getMaterialWeight().isBlank()) {
            return product.getMaterialWeight();
        }

        if (product.getDescription() == null) return "";

        String desc = product.getDescription();
        // Try to extract weight patterns like "7.98gr", "10.11gr", etc.
        if (desc.matches(".*\\d+\\.?\\d*gr.*")) {
            String[] parts = desc.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+\\.?\\d*gr")) {
                    return part.replace("gr", "");
                }
            }
        }

        return "";
    }

    /**
     * Extract material information from product data
     */
    private String extractMaterial(MirrorProduct product) {
        // First check metalType field
        if (product.getMetalType() != null && !product.getMetalType().isBlank()) {
            return product.getMetalType();
        }

        if (product.getDescription() == null) return "";

        String desc = product.getDescription().toLowerCase();
        if (desc.contains("18k white gold")) return "18K White Gold";
        if (desc.contains("18k yellow gold")) return "18K Yellow Gold";
        if (desc.contains("18k rose gold")) return "18K Rose Gold";
        if (desc.contains("18k gold")) return "18K Gold";
        if (desc.contains("microfiber")) return "Microfiber";
        if (desc.contains("leatherette")) return "Leatherette";
        if (desc.contains("acrylic")) return "Acrylic";
        if (desc.contains("velvet")) return "Velvet";
        if (desc.contains("suede")) return "Suede";

        return "";
    }

    /**
     * Generate filename for MISA export
     */
    public String generateExportFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("MISA_Import_%s.xls", timestamp);
    }
}
