package com.mirror.product.service;

import com.mirror.product.dto.sku.SkuGenerationResult;
import com.mirror.product.entity.JTRCMetalComponent;
import com.mirror.product.entity.JTRCStoneComponent;
import com.mirror.product.entity.JewelryTechnicalReport;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.enums.JTRCStatus;
import com.mirror.product.enums.ProductStatus;
import com.mirror.product.enums.StoneColorCategory;
import com.mirror.product.enums.StoneRole;
import com.mirror.product.repository.JewelryTechnicalReportRepository;
import com.mirror.product.repository.MirrorProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("JTRC Product Link Service Tests")
@ExtendWith(MockitoExtension.class)
class JTRCProductLinkServiceTest {

    @Mock
    private MirrorProductRepository mirrorProductRepository;

    @Mock
    private JewelryTechnicalReportRepository jtrcRepository;

    @Mock
    private SkuCodeService skuCodeService;

    @Mock
    private BarcodeGenerationService barcodeGenerationService;

    @InjectMocks
    private JTRCProductLinkService service;

    // Test data - uses canonical codes (no underscores, no spaces)
    private static final String JTRC_ID = "JTR000001";
    private static final String REPORT_NUMBER = "JTRC-2026-SPR-001";
    private static final String PRODUCT_ID = "PRD000001";
    private static final String BARCODE = "7676950963035";
    private static final String DESCRIPTIVE_CODE = "RNG-18KWG-W-2.09-LG-RD-1.29-N";

    private JewelryTechnicalReport buildFullJtrc() {
        JTRCMetalComponent metal = JTRCMetalComponent.builder()
                .metalType("WHITEGOLD")
                .metalPurity("18K")
                .weightGrams(new BigDecimal("2.09"))
                .pricePerGram(new BigDecimal("1850000"))
                .build();

        JTRCStoneComponent mainStone = JTRCStoneComponent.builder()
                .stoneRole(StoneRole.MAIN)
                .stoneType("LABDIAMOND")
                .shape("ROUND")
                .colorCategory(StoneColorCategory.COLORLESS)
                .colorGrade("F")
                .clarity("VS1")
                .weightCarat(new BigDecimal("1.29"))
                .quantity(1)
                .build();

        JTRCStoneComponent sideStone = JTRCStoneComponent.builder()
                .stoneRole(StoneRole.SIDE)
                .stoneType("LABDIAMOND")
                .shape("ROUND")
                .colorCategory(StoneColorCategory.COLORLESS)
                .colorGrade("G")
                .weightCarat(new BigDecimal("0.03"))
                .quantity(12)
                .build();

        Set<JTRCStoneComponent> stones = new HashSet<>();
        stones.add(mainStone);
        stones.add(sideStone);

        JewelryTechnicalReport jtrc = JewelryTechnicalReport.builder()
                .reportNumber(REPORT_NUMBER)
                .category("RNG")
                .collection("Spring 2026")
                .season("SS26")
                .status(JTRCStatus.APPROVED)
                .metalComponent(metal)
                .stoneComponents(stones)
                .laborComponents(new HashSet<>())
                .totalCogsVnd(new BigDecimal("45000000"))
                .build();
        jtrc.setId(JTRC_ID);

        metal.setJtrc(jtrc);
        mainStone.setJtrc(jtrc);
        sideStone.setJtrc(jtrc);

        return jtrc;
    }

    private MirrorProduct buildExistingProduct() {
        MirrorProduct product = new MirrorProduct();
        product.setId(PRODUCT_ID);
        product.setSkuCode(BARCODE);
        product.setDescriptiveCode(DESCRIPTIVE_CODE);
        product.setCategory("RNG");
        product.setStatus(ProductStatus.DRAFT);
        return product;
    }

    @Nested
    @DisplayName("ensureProductLinked")
    class EnsureProductLinked {

        @Test
        @DisplayName("should return existing product when JTRC already has valid productId")
        void existingProductLink() {
            JewelryTechnicalReport jtrc = buildFullJtrc();
            jtrc.setProductId(PRODUCT_ID);
            MirrorProduct existingProduct = buildExistingProduct();

            when(mirrorProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

            MirrorProduct result = service.ensureProductLinked(jtrc);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PRODUCT_ID);
            verify(mirrorProductRepository, never()).findByDescriptiveCode(anyString());
            verify(mirrorProductRepository, never()).save(any());
        }

        @Test
        @DisplayName("should re-link when JTRC has productId but product was deleted")
        void relinksWhenProductDeleted() {
            JewelryTechnicalReport jtrc = buildFullJtrc();
            jtrc.setProductId("DELETED-ID");

            when(mirrorProductRepository.findById("DELETED-ID")).thenReturn(Optional.empty());
            when(skuCodeService.generateJewelrySku(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new SkuGenerationResult(DESCRIPTIVE_CODE, false));
            when(mirrorProductRepository.findByDescriptiveCode(DESCRIPTIVE_CODE)).thenReturn(Optional.empty());
            when(barcodeGenerationService.generateUniqueBarcode()).thenReturn(BARCODE);
            when(mirrorProductRepository.save(any(MirrorProduct.class))).thenAnswer(inv -> {
                MirrorProduct p = inv.getArgument(0);
                if (p.getId() == null) p.setId("PRD000002");
                return p;
            });
            when(jtrcRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MirrorProduct result = service.ensureProductLinked(jtrc);

            assertThat(result).isNotNull();
            assertThat(jtrc.getProductId()).isEqualTo(result.getId());
        }

        @Test
        @DisplayName("should match existing product by descriptiveCode")
        void matchByDescriptiveCode() {
            JewelryTechnicalReport jtrc = buildFullJtrc();
            MirrorProduct existingProduct = buildExistingProduct();

            when(skuCodeService.generateJewelrySku(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new SkuGenerationResult(DESCRIPTIVE_CODE, false));
            when(mirrorProductRepository.findByDescriptiveCode(DESCRIPTIVE_CODE)).thenReturn(Optional.of(existingProduct));
            when(jtrcRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MirrorProduct result = service.ensureProductLinked(jtrc);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PRODUCT_ID);
            assertThat(jtrc.getProductId()).isEqualTo(PRODUCT_ID);
            verify(mirrorProductRepository, never()).save(any()); // no new product created
        }

        @Test
        @DisplayName("should create new product when no match found")
        void createNewProduct() {
            JewelryTechnicalReport jtrc = buildFullJtrc();

            when(skuCodeService.generateJewelrySku(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new SkuGenerationResult(DESCRIPTIVE_CODE, false));
            when(mirrorProductRepository.findByDescriptiveCode(DESCRIPTIVE_CODE)).thenReturn(Optional.empty());
            when(barcodeGenerationService.generateUniqueBarcode()).thenReturn(BARCODE);
            when(mirrorProductRepository.save(any(MirrorProduct.class))).thenAnswer(inv -> {
                MirrorProduct p = inv.getArgument(0);
                if (p.getId() == null) p.setId("PRD000003");
                return p;
            });
            when(jtrcRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MirrorProduct result = service.ensureProductLinked(jtrc);

            assertThat(result).isNotNull();
            assertThat(result.getSkuCode()).isEqualTo(BARCODE);
            assertThat(result.getDescriptiveCode()).isEqualTo(DESCRIPTIVE_CODE);
            assertThat(result.getStatus()).isEqualTo(ProductStatus.DRAFT);

            // Verify mapped attributes - now using canonical codes
            ArgumentCaptor<MirrorProduct> captor = ArgumentCaptor.forClass(MirrorProduct.class);
            verify(mirrorProductRepository).save(captor.capture());
            MirrorProduct saved = captor.getValue();

            assertThat(saved.getCategory()).isEqualTo("RNG");
            assertThat(saved.getMetalType()).isEqualTo("WHITEGOLD");
            assertThat(saved.getMetalPurity()).isEqualTo("18K");
            assertThat(saved.getMaterialColor()).isEqualTo("WHITE");
            assertThat(saved.getMaterialWeight()).isEqualTo("2.09");
            assertThat(saved.getStoneType()).isEqualTo("LABDIAMOND");
            assertThat(saved.getStoneOrigin()).isEqualTo("LABGROWN");
            assertThat(saved.getStoneShape()).isEqualTo("ROUND");
            assertThat(saved.getStoneWeight()).isEqualTo("1.29");
            assertThat(saved.getSideStones()).isEqualTo("DIAMONDS");
            assertThat(saved.getCost()).isEqualByComparingTo(new BigDecimal("45000000"));
        }

        @Test
        @DisplayName("should return null when JTRC has no metal component")
        void noMetalComponent() {
            JewelryTechnicalReport jtrc = buildFullJtrc();
            jtrc.setMetalComponent(null);

            MirrorProduct result = service.ensureProductLinked(jtrc);

            assertThat(result).isNull();
            verify(mirrorProductRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deriveDescriptiveCode")
    class DeriveDescriptiveCode {

        @Test
        @DisplayName("should generate full descriptive code with metal + main stone + side stones")
        void fullSpecs() {
            JewelryTechnicalReport jtrc = buildFullJtrc();

            when(skuCodeService.generateJewelrySku(
                    eq("RNG"), anyString(), eq("WHITE"), eq("2.09"),
                    isNull(), isNull(),
                    eq("LABGROWN"), eq("ROUND"), eq("1.29"), eq("DIAMONDS"), isNull()
            )).thenReturn(new SkuGenerationResult(DESCRIPTIVE_CODE, false));

            String code = service.deriveDescriptiveCode(jtrc);

            assertThat(code).isEqualTo(DESCRIPTIVE_CODE);
        }

        @Test
        @DisplayName("should generate code without stone parts when no stones")
        void noStones() {
            JewelryTechnicalReport jtrc = buildFullJtrc();
            jtrc.setStoneComponents(new HashSet<>());

            when(skuCodeService.generateJewelrySku(
                    eq("RNG"), anyString(), eq("WHITE"), eq("2.09"),
                    isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), isNull()
            )).thenReturn(new SkuGenerationResult("RNG-18KWG-W-2.09", false));

            String code = service.deriveDescriptiveCode(jtrc);

            assertThat(code).isEqualTo("RNG-18KWG-W-2.09");
        }

        @Test
        @DisplayName("should return null when no metal component")
        void noMetal() {
            JewelryTechnicalReport jtrc = buildFullJtrc();
            jtrc.setMetalComponent(null);

            String code = service.deriveDescriptiveCode(jtrc);

            assertThat(code).isNull();
            verifyNoInteractions(skuCodeService);
        }
    }

    @Nested
    @DisplayName("Material color extraction")
    class MaterialColor {

        @Test
        @DisplayName("should extract color from canonical metal type codes")
        void extractColors() {
            assertThat(service.deriveMaterialColor("WHITEGOLD")).isEqualTo("WHITE");
            assertThat(service.deriveMaterialColor("YELLOWGOLD")).isEqualTo("YELLOW");
            assertThat(service.deriveMaterialColor("ROSEGOLD")).isEqualTo("ROSE");
        }

        @Test
        @DisplayName("should return null for unrecognized metal types")
        void unknownMetal() {
            assertThat(service.deriveMaterialColor("PLATINUM")).isNull();
            assertThat(service.deriveMaterialColor(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Stone origin extraction")
    class StoneOrigin {

        @Test
        @DisplayName("should derive origin from canonical stone type codes")
        void extractOrigin() {
            assertThat(service.deriveStoneOrigin("LABDIAMOND")).isEqualTo("LABGROWN");
            assertThat(service.deriveStoneOrigin("NATURALDIAMOND")).isEqualTo("NATURAL");
        }

        @Test
        @DisplayName("should return null for non-diamond stones")
        void nonDiamond() {
            assertThat(service.deriveStoneOrigin("RUBY")).isNull();
            assertThat(service.deriveStoneOrigin("MOISSANITE")).isNull();
            assertThat(service.deriveStoneOrigin(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Main stone selection")
    class MainStoneSelection {

        @Test
        @DisplayName("should prefer MAIN over MAIN_2")
        void prefersMain() {
            JewelryTechnicalReport jtrc = buildFullJtrc();

            JTRCStoneComponent main2 = JTRCStoneComponent.builder()
                    .stoneRole(StoneRole.MAIN_2)
                    .stoneType("NATURALDIAMOND")
                    .shape("PRINCESS")
                    .colorCategory(StoneColorCategory.COLORLESS)
                    .colorGrade("E")
                    .weightCarat(new BigDecimal("0.50"))
                    .quantity(1)
                    .build();
            main2.setJtrc(jtrc);
            jtrc.getStoneComponents().add(main2);

            when(skuCodeService.generateJewelrySku(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new SkuGenerationResult(DESCRIPTIVE_CODE, false));

            service.deriveDescriptiveCode(jtrc);

            // Verify the MAIN stone's shape was used, not MAIN_2
            verify(skuCodeService).generateJewelrySku(
                    any(), any(), any(), any(), any(), any(),
                    eq("LABGROWN"), eq("ROUND"), eq("1.29"), any(), any()
            );
        }
    }
}
