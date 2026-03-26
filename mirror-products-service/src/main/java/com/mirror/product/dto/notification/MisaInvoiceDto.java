package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MisaInvoiceDto {

    @JsonProperty("InvoiceId")
    private String id;

    @JsonProperty("InvoiceNumber")
    private String code;

    @JsonProperty("InvoiceType")
    private Integer invoiceType;

    @JsonProperty("CustomerID")
    private String customerId;

    @JsonProperty("CustomerName")
    private String customerName;

    @JsonProperty("Tel")
    private String customerPhone;

    @JsonProperty("Address")
    private String customerAddress;

    @JsonProperty("BranchId")
    private String branchId;

    @JsonProperty("BranchName")
    private String branchName;

    @JsonProperty("InvoiceDate")
    private OffsetDateTime invoiceDate;

    @JsonProperty("TotalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("DiscountAmount")
    private BigDecimal discountAmount;

    @JsonProperty("TaxAmount")
    private BigDecimal taxAmount;

    @JsonProperty("ActualAmount")
    private BigDecimal finalAmount;

    @JsonProperty("DebitAmount")
    private BigDecimal debtAmount;

    @JsonProperty("CashAmount")
    private BigDecimal cashAmount;

    @JsonProperty("CardAmount")
    private BigDecimal cardAmount;

    @JsonProperty("VoucherAmount")
    private BigDecimal voucherAmount;

    @JsonProperty("PaymentStatus")
    private Integer paymentStatus;

    @JsonProperty("Cashier")
    private String cashier;

    @JsonProperty("SaleStaff")
    private String saleStaff;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Note")
    private String note;

    @JsonProperty("DeliveryCode")
    private String deliveryCode;

    @JsonProperty("ShippingPartnerName")
    private String shippingPartnerName;

    @JsonProperty("InvoiceDetails")
    private List<InvoiceDetailDto> invoiceDetails;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceDetailDto {

        @JsonProperty("Id")
        private String id;

        @JsonProperty("InvoiceId")
        private String invoiceId;

        @JsonProperty("ProductId")
        private String productId;

        @JsonProperty("ProductCode")
        private String productCode;

        @JsonProperty("ProductName")
        private String productName;

        @JsonProperty("UnitId")
        private String unitId;

        @JsonProperty("UnitName")
        private String unitName;

        @JsonProperty("Quantity")
        private BigDecimal quantity;

        @JsonProperty("UnitPrice")
        private BigDecimal unitPrice;

        @JsonProperty("DiscountRate")
        private BigDecimal discountRate;

        @JsonProperty("DiscountAmount")
        private BigDecimal discountAmount;

        @JsonProperty("TaxRate")
        private BigDecimal taxRate;

        @JsonProperty("TaxAmount")
        private BigDecimal taxAmount;

        @JsonProperty("TotalAmount")
        private BigDecimal totalAmount;

        @JsonProperty("Description")
        private String description;
    }
}
