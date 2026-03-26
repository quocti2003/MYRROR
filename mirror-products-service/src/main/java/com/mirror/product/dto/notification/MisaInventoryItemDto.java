package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MisaInventoryItemDto {

    @JsonProperty("AvgUnitPrice")
    private BigDecimal avgUnitPrice;

    @JsonProperty("ListDetail")
    private List<ListDetailDto> listDetail;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("BranchId")
    private String branchId;

    @JsonProperty("ItemType")
    private Integer itemType;

    @JsonProperty("ItemCategoryId")
    private String itemCategoryId;

    @JsonProperty("SellingPrice")
    private BigDecimal sellingPrice;

    @JsonProperty("CostPrice")
    private BigDecimal costPrice;

    @JsonProperty("Color")
    private String color;

    @JsonProperty("ColourCode")
    private String colourCode;

    @JsonProperty("Size")
    private String size;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("IsItem")
    private Boolean isItem;

    @JsonProperty("Inactive")
    private Boolean inactive;

    @JsonProperty("UnitId")
    private String unitId;

    @JsonProperty("UnitName")
    private String unitName;

    @JsonProperty("ItemCategoryName")
    private String itemCategoryName;

    @JsonProperty("Picture")
    private String picture;

    @JsonProperty("ModifiedDate")
    private OffsetDateTime modifiedDate;

    @JsonProperty("ListPictureUrl")
    private List<String> listPictureUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListDetailDto {

        @JsonProperty("IsComboItem")
        private Boolean isComboItem;

        @JsonProperty("ComboId")
        private Integer comboId;

        @JsonProperty("ComboCostPrice")
        private BigDecimal comboCostPrice;

        @JsonProperty("ComboSalePrice")
        private BigDecimal comboSalePrice;

        @JsonProperty("ComboQuantity")
        private BigDecimal comboQuantity;

        @JsonProperty("Id")
        private String id;

        @JsonProperty("Code")
        private String code;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("BranchId")
        private String branchId;

        @JsonProperty("ItemType")
        private Integer itemType;

        @JsonProperty("Barcode")
        private String barcode;

        @JsonProperty("SellingPrice")
        private BigDecimal sellingPrice;

        @JsonProperty("CostPrice")
        private BigDecimal costPrice;

        @JsonProperty("Color")
        private String color;

        @JsonProperty("ColourCode")
        private String colourCode;

        @JsonProperty("Size")
        private String size;

        @JsonProperty("Material")
        private String material;

        @JsonProperty("IsItem")
        private Boolean isItem;

        @JsonProperty("Inactive")
        private Boolean inactive;

        @JsonProperty("UnitId")
        private String unitId;

        @JsonProperty("UnitName")
        private String unitName;

        @JsonProperty("Inventories")
        private List<InventoryDto> inventories;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InventoryDto {

        @JsonProperty("ProductId")
        private String productId;

        @JsonProperty("ProductCode")
        private String productCode;

        @JsonProperty("ProductName")
        private String productName;

        @JsonProperty("BranchId")
        private String branchId;

        @JsonProperty("BranchName")
        private String branchName;

        @JsonProperty("SellingPrice")
        private BigDecimal sellingPrice;

        @JsonProperty("OnHand")
        private BigDecimal onHand;

        @JsonProperty("Ordered")
        private BigDecimal ordered;

        @JsonProperty("PreOrdered")
        private BigDecimal preOrdered;
    }
}
