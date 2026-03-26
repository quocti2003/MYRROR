package com.mirror.product.service;

import com.mirror.product.dto.sku.BulkSkuGenerationResponse;
import com.mirror.product.dto.sku.GeneratedSkuResponse;
import com.mirror.product.dto.sku.JewelrySkuRequest;
import com.mirror.product.dto.sku.PackagingSkuRequest;
import com.mirror.product.dto.sku.SkuGenerationResult;
import com.mirror.product.util.NumberValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkuImportService {

    private final SkuCodeService skuCodeService;
    private final GeneratedSkuService generatedSkuService;

    public BulkSkuGenerationResponse importJewelrySkus(MultipartFile file) {
        return processCsv(file, this::handleJewelryRecord);
    }

    public BulkSkuGenerationResponse importPackagingSkus(MultipartFile file) {
        return processCsv(file, this::handlePackagingRecord);
    }

    private BulkSkuGenerationResponse processCsv(
            MultipartFile file,
            RecordProcessor processor) {

        List<GeneratedSkuResponse> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withTrim()
                 .parse(reader)) {

            for (CSVRecord record : parser) {
                totalRows++;
                try {
                    GeneratedSkuResponse response = processor.process(record, totalRows);
                    if (response != null) {
                        successes.add(response);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to import row {}: {}", totalRows, ex.getMessage());
                    errors.add(String.format("Row %d: %s", totalRows, ex.getMessage()));
                }
            }

        } catch (IOException ioException) {
            String message = "Unable to read uploaded CSV file";
            log.error(message, ioException);
            errors.add(message + ": " + ioException.getMessage());
        }

        return BulkSkuGenerationResponse.builder()
            .totalRows(totalRows)
            .successCount(successes.size())
            .failureCount(totalRows - successes.size())
            .generatedCodes(successes)
            .errors(errors)
            .build();
    }

    private GeneratedSkuResponse handleJewelryRecord(CSVRecord record, int rowNumber) {
        String prefix = getColumn(record, "prefix");
        if (!StringUtils.hasText(prefix)) {
            throw new IllegalArgumentException("Missing prefix");
        }
        prefix = prefix.toUpperCase();

        String countryOfOrigin = getColumn(record, "country_of_origin");
        if (!StringUtils.hasText(countryOfOrigin)) {
            throw new IllegalArgumentException("Missing country_of_origin");
        }
        countryOfOrigin = countryOfOrigin.toUpperCase();

        String itemName = getColumn(record, "item_name");
        if (!StringUtils.hasText(itemName)) {
            throw new IllegalArgumentException("Missing item_name");
        }

        String materialColor = getColumn(record, "material_color");
        if (!StringUtils.hasText(materialColor)) {
            throw new IllegalArgumentException("Missing material_color");
        }

        String materialWeight = getColumn(record, "material_weight");
        if (!StringUtils.hasText(materialWeight)) {
            throw new IllegalArgumentException("Missing material_weight");
        }
        // Validate and round material weight (must be > 0, < 100000, rounded to 2 decimals)
        materialWeight = NumberValidationUtil.validateAndRoundWeight(materialWeight);

        String isCoatedStr = getColumn(record, "is_coated");
        Boolean isCoated = Boolean.parseBoolean(isCoatedStr);

        String coatingMaterial = getColumn(record, "coating_material");

        JewelrySkuRequest request = new JewelrySkuRequest();
        request.setPrefix(prefix);
        request.setMaterial(resolveMaterial(record));
        request.setMaterialColor(materialColor.toUpperCase());
        request.setMaterialWeight(materialWeight);
        request.setIsCoated(isCoated);
        request.setCoatingMaterial(coatingMaterial != null ? coatingMaterial.toUpperCase() : null);
        request.setOrigin(resolveOrigin(record));
        request.setShape(getColumn(record, "main_shape"));
        request.setWeight(getColumn(record, "main_weight"));
        request.setSideStones(getColumn(record, "side_stones"));
        request.setCountryOfOrigin(countryOfOrigin);
        request.setItemName(itemName);
        request.setVariant(getColumn(record, "variant"));

        SkuGenerationResult result = skuCodeService.generateJewelrySku(
            request.getPrefix(),
            request.getMaterial(),
            request.getMaterialColor(),
            request.getMaterialWeight(),
            request.getIsCoated(),
            request.getCoatingMaterial(),
            request.getOrigin(),
            request.getShape(),
            request.getWeight(),
            request.getSideStones(),
            request.getVariant()
        );

        log.debug("Generated jewelry SKU {} from row {}", result.getCode(), rowNumber);
        return generatedSkuService.recordJewelrySku(result, request);
    }

    private GeneratedSkuResponse handlePackagingRecord(CSVRecord record, int rowNumber) {
        String prefix = getColumn(record, "prefix");
        if (!StringUtils.hasText(prefix)) {
            throw new IllegalArgumentException("Missing prefix");
        }
        prefix = prefix.toUpperCase();

        String countryOfOrigin = getColumn(record, "country_of_origin");
        if (!StringUtils.hasText(countryOfOrigin)) {
            throw new IllegalArgumentException("Missing country_of_origin");
        }
        countryOfOrigin = countryOfOrigin.toUpperCase();

        String itemName = getColumn(record, "item_name");
        if (!StringUtils.hasText(itemName)) {
            throw new IllegalArgumentException("Missing item_name");
        }

        PackagingSkuRequest request = new PackagingSkuRequest();
        request.setPrefix(prefix);
        request.setMaterial(resolvePackagingMaterial(record));
        request.setSize(getColumn(record, "size_mm"));
        request.setColor(getColumn(record, "insert"));
        request.setType(getColumn(record, "type"));
        request.setFinish(getColumn(record, "logo"));
        request.setCountryOfOrigin(countryOfOrigin);
        request.setItemName(itemName);
        request.setNotes(getColumn(record, "remarks"));

        SkuGenerationResult result = skuCodeService.generatePackagingSku(
            request.getPrefix(),
            request.getMaterial(),
            request.getSize(),
            request.getColor(),
            request.getType(),
            request.getFinish(),
            request.getNotes()
        );

        log.debug("Generated packaging SKU {} from row {}", result.getCode(), rowNumber);
        return generatedSkuService.recordPackagingSku(result, request);
    }

    private String resolveMaterial(CSVRecord record) {
        String material = getColumn(record, "material");
        if (StringUtils.hasText(material)) {
            return material;
        }

        String description = getColumn(record, "full_description");
        if (!StringUtils.hasText(description)) {
            description = getColumn(record, "item_name");
        }

        return skuCodeService.extractMaterialFromDescription(description);
    }

    private String resolveOrigin(CSVRecord record) {
        String origin = getColumn(record, "stone_origin");
        if (StringUtils.hasText(origin)) {
            return origin;
        }

        String description = getColumn(record, "full_description");
        if (!StringUtils.hasText(description)) {
            description = getColumn(record, "item_name");
        }

        return skuCodeService.extractOriginFromDescription(description);
    }

    private String resolvePackagingMaterial(CSVRecord record) {
        String material = getColumn(record, "material");
        if (StringUtils.hasText(material)) {
            return material;
        }

        String itemName = getColumn(record, "item_name");
        String description = getColumn(record, "description");

        if (StringUtils.hasText(itemName)) {
            String detected = detectPackagingMaterial(itemName);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        }

        if (StringUtils.hasText(description)) {
            String detected = detectPackagingMaterial(description);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        }

        return "";
    }

    private String detectPackagingMaterial(String text) {
        String lowered = text.toLowerCase();
        if (lowered.contains("microfiber")) {
            return "MICROFIBER";
        }
        if (lowered.contains("leather")) {
            return "LEATHERETTE";
        }
        if (lowered.contains("acrylic")) {
            return "ACRYLIC";
        }
        if (lowered.contains("velvet")) {
            return "VELVET";
        }
        if (lowered.contains("suede")) {
            return "SUEDE";
        }
        if (lowered.contains("wood")) {
            return "WOOD";
        }
        return "";
    }

    private String getColumn(CSVRecord record, String column) {
        if (record.isMapped(column)) {
            String value = record.get(column);
            return value != null ? value.trim() : "";
        }
        return "";
    }

    @FunctionalInterface
    private interface RecordProcessor {
        GeneratedSkuResponse process(CSVRecord record, int rowNumber);
    }
}
