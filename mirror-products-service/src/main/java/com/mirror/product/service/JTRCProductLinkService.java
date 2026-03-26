package com.mirror.product.service;

import com.mirror.product.dto.sku.SkuGenerationResult;
import com.mirror.product.entity.JTRCMetalComponent;
import com.mirror.product.entity.JTRCStoneComponent;
import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.enums.StoneRole;
import com.mirror.product.repository.JewelryTechnicalReportRepository;
import com.mirror.product.repository.MirrorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/**
 * Service for automatically linking JTRCs to MirrorProducts.
 *
 * When a JTRC enters the production pipeline (via order generation),
 * this service ensures a corresponding MirrorProduct exists:
 * 1. Derives a descriptiveCode from JTRC components (metal + stones)
 * 2. Looks up existing product by descriptiveCode
 * 3. If found → links JTRC to that product
 * 4. If not found → creates a new MirrorProduct, then links
 *
 * Now uses canonical dropdown codes directly (e.g., "RNG", "WHITEGOLD", "LABDIAMOND")
 * — no mapping needed since JTRC stores the same codes as the SKU generator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JTRCProductLinkService {

    private final MirrorProductRepository mirrorProductRepository;
    private final JewelryTechnicalReportRepository jtrcRepository;
    private final SkuCodeService skuCodeService;
    private final BarcodeGenerationService barcodeGenerationService;

    /**
     * Ensures a JTRC has a linked MirrorProduct.
     * If already linked and the product exists, returns it.
     * Otherwise derives descriptiveCode, matches or creates a product.
     *
     * @param jtrc The JTRC (must have components loaded)
     * @return The linked MirrorProduct, or null if descriptiveCode cannot be derived
     */
    @Transactional
    public MirrorProduct ensureProductLinked(JewelryTechnicalReport jtrc) {
        // If already linked, verify the product still exists
        if (StringUtils.hasText(jtrc.getProductId())) {
            Optional<MirrorProduct> existing = mirrorProductRepository.findById(jtrc.getProductId());
            if (existing.isPresent()) {
                log.debug("JTRC {} already linked to product {}", jtrc.getReportNumber(), jtrc.getProductId());
                return existing.get();
            }
            log.warn("JTRC {} has productId {} but product not found, will re-link",
                    jtrc.getReportNumber(), jtrc.getProductId());
        }

        // Derive descriptive code from JTRC components
        String descriptiveCode = deriveDescriptiveCode(jtrc);
        if (descriptiveCode == null) {
            log.warn("Cannot derive descriptiveCode for JTRC {} - missing metal component", jtrc.getReportNumber());
            return null;
        }

        // Look for existing product by descriptiveCode
        Optional<MirrorProduct> matchingProduct = mirrorProductRepository.findByDescriptiveCode(descriptiveCode);

        MirrorProduct product;
        if (matchingProduct.isPresent()) {
            product = matchingProduct.get();
            log.info("JTRC {} matched existing product {} by descriptiveCode {}",
                    jtrc.getReportNumber(), product.getId(), descriptiveCode);
        } else {
            product = createProductFromJtrc(jtrc, descriptiveCode);
            log.info("Created new product {} for JTRC {} with descriptiveCode {}",
                    product.getId(), jtrc.getReportNumber(), descriptiveCode);
        }

        // Link JTRC to product
        jtrc.setProductId(product.getId());
        jtrcRepository.save(jtrc);

        return product;
    }

    /**
     * Derives a descriptive code from JTRC components using SkuCodeService.
     * JTRC now stores canonical codes (e.g., category="RNG", metalType="WHITEGOLD")
     * so no mapping is needed.
     *
     * @return The descriptive code string, or null if metal component is missing
     */
    public String deriveDescriptiveCode(JewelryTechnicalReport jtrc) {
        JTRCMetalComponent metal = jtrc.getMetalComponent();
        if (metal == null) {
            return null;
        }

        // Category IS the prefix code now (e.g., "RNG")
        String prefix = jtrc.getCategory();
        if (!StringUtils.hasText(prefix)) {
            prefix = "OTH";
        }

        // Concatenate purity + metalType: "18K" + "WHITEGOLD" = "18KWHITEGOLD"
        String material = deriveMaterial(metal);
        String materialColor = deriveMaterialColor(metal.getMetalType());
        String materialWeight = metal.getWeightGrams() != null
                ? metal.getWeightGrams().stripTrailingZeros().toPlainString()
                : null;

        // Find the main stone
        JTRCStoneComponent mainStone = findMainStone(jtrc);

        String origin = null;
        String shape = null;
        String stoneWeight = null;
        String sideStones = null;

        if (mainStone != null) {
            origin = deriveStoneOrigin(mainStone.getStoneType());
            shape = mainStone.getShape();
            stoneWeight = mainStone.getWeightCarat() != null
                    ? mainStone.getWeightCarat().stripTrailingZeros().toPlainString()
                    : null;
        }

        // Check for side stones
        if (jtrc.getStoneComponents() != null) {
            boolean hasSideStones = jtrc.getStoneComponents().stream()
                    .anyMatch(s -> s.getStoneRole() == StoneRole.SIDE
                            || s.getStoneRole() == StoneRole.SIDE_2
                            || s.getStoneRole() == StoneRole.MELEE);
            sideStones = hasSideStones ? "DIAMONDS" : (mainStone != null ? "NONE" : null);
        }

        SkuGenerationResult result = skuCodeService.generateJewelrySku(
                prefix, material, materialColor, materialWeight,
                null, null, // coating not tracked in JTRC
                origin, shape, stoneWeight, sideStones, null
        );

        String code = result.getCode();
        return StringUtils.hasText(code) ? code : null;
    }

    /**
     * Creates a new MirrorProduct from JTRC data.
     */
    private MirrorProduct createProductFromJtrc(JewelryTechnicalReport jtrc, String descriptiveCode) {
        MirrorProduct product = new MirrorProduct();

        // Generate unique barcode/SKU
        String barcode = barcodeGenerationService.generateUniqueBarcode();
        product.setSkuCode(barcode);
        product.setBarcode(barcode);
        product.setDescriptiveCode(descriptiveCode);

        // Category is already a prefix code (e.g., "RNG")
        product.setCategory(jtrc.getCategory());

        // Metal attributes
        JTRCMetalComponent metal = jtrc.getMetalComponent();
        if (metal != null) {
            product.setMetalType(metal.getMetalType());
            product.setMetalPurity(metal.getMetalPurity());
            product.setMaterialColor(deriveMaterialColor(metal.getMetalType()));
            product.setWeightGrams(metal.getWeightGrams());
            if (metal.getWeightGrams() != null) {
                product.setMaterialWeight(metal.getWeightGrams().stripTrailingZeros().toPlainString());
            }
        }

        // Main stone attributes
        JTRCStoneComponent mainStone = findMainStone(jtrc);
        if (mainStone != null) {
            product.setStoneType(mainStone.getStoneType());
            product.setStoneOrigin(deriveStoneOrigin(mainStone.getStoneType()));
            product.setStoneShape(mainStone.getShape());
            if (mainStone.getWeightCarat() != null) {
                product.setStoneWeight(mainStone.getWeightCarat().stripTrailingZeros().toPlainString());
            }
        }

        // Side stones
        if (jtrc.getStoneComponents() != null) {
            boolean hasSideStones = jtrc.getStoneComponents().stream()
                    .anyMatch(s -> s.getStoneRole() == StoneRole.SIDE
                            || s.getStoneRole() == StoneRole.SIDE_2
                            || s.getStoneRole() == StoneRole.MELEE);
            product.setSideStones(hasSideStones ? "DIAMONDS" : "NONE");
        }

        // Cost from JTRC totals
        if (jtrc.getTotalCogsVnd() != null) {
            product.setCost(jtrc.getTotalCogsVnd());
        }

        // Build item name
        product.setItemName(buildItemName(jtrc, mainStone));
        product.setDescription(String.format("Auto-generated from JTRC %s", jtrc.getReportNumber()));

        product.setStatus(ProductStatus.DRAFT);

        return mirrorProductRepository.save(product);
    }

    // ==================== Helper Methods ====================

    /**
     * Combines metalPurity + metalType into a material string for SKU generation.
     * With canonical codes: "18K" + "WHITEGOLD" = "18KWHITEGOLD" (matches material_options directly)
     */
    String deriveMaterial(JTRCMetalComponent metal) {
        String purity = metal.getMetalPurity();
        String type = metal.getMetalType();

        if (StringUtils.hasText(purity) && StringUtils.hasText(type)) {
            // If type already contains purity (e.g., "18KWHITEGOLD"), use type directly
            if (type.toUpperCase().contains(purity.toUpperCase())) {
                return type;
            }
            return purity + type;
        }
        return StringUtils.hasText(type) ? type : purity;
    }

    /**
     * Extracts material color from metal type code.
     * WHITEGOLD contains "WHITE" → WHITE
     * YELLOWGOLD contains "YELLOW" → YELLOW
     * ROSEGOLD contains "ROSE" → ROSE
     */
    String deriveMaterialColor(String metalType) {
        if (metalType == null) return null;
        String upper = metalType.toUpperCase();
        if (upper.contains("WHITE")) return "WHITE";
        if (upper.contains("YELLOW")) return "YELLOW";
        if (upper.contains("ROSE")) return "ROSE";
        if (upper.contains("PINK")) return "PINK";
        return null;
    }

    /**
     * Derives stone origin from the stone type code.
     * LABDIAMOND starts with "LAB" → "LABGROWN"
     * NATURALDIAMOND starts with "NATURAL" → "NATURAL"
     */
    String deriveStoneOrigin(String stoneType) {
        if (stoneType == null) return null;
        String upper = stoneType.toUpperCase();
        if (upper.startsWith("LAB")) return "LABGROWN";
        if (upper.startsWith("NATURAL")) return "NATURAL";
        return null;
    }

    /**
     * Finds the primary main stone from JTRC stone components.
     * Priority: MAIN > MAIN_2
     */
    private JTRCStoneComponent findMainStone(JewelryTechnicalReport jtrc) {
        Set<JTRCStoneComponent> stones = jtrc.getStoneComponents();
        if (stones == null || stones.isEmpty()) return null;

        return stones.stream()
                .filter(s -> s.getStoneRole() == StoneRole.MAIN || s.getStoneRole() == StoneRole.MAIN_2)
                .min(Comparator.comparingInt(s -> s.getStoneRole().ordinal()))
                .orElse(null);
    }

    /**
     * Builds a human-readable product name from JTRC data.
     */
    private String buildItemName(JewelryTechnicalReport jtrc, JTRCStoneComponent mainStone) {
        StringBuilder name = new StringBuilder();

        // Category
        name.append(jtrc.getCategory() != null ? jtrc.getCategory() : "Jewelry");

        // Metal
        JTRCMetalComponent metal = jtrc.getMetalComponent();
        if (metal != null) {
            if (StringUtils.hasText(metal.getMetalPurity())) {
                name.append(" - ").append(metal.getMetalPurity());
            }
            if (StringUtils.hasText(metal.getMetalType())) {
                name.append(" ").append(metal.getMetalType());
            }
        }

        // Main stone
        if (mainStone != null) {
            name.append(" - ");
            if (mainStone.getWeightCarat() != null) {
                name.append(mainStone.getWeightCarat().stripTrailingZeros().toPlainString()).append("ct ");
            }
            if (StringUtils.hasText(mainStone.getShape())) {
                name.append(mainStone.getShape());
            }
            if (StringUtils.hasText(mainStone.getStoneType())) {
                name.append(" ").append(mainStone.getStoneType());
            }
        }

        return name.toString();
    }
}
