package com.mirror.product.enums;

public enum EntityPrefix {
    CAT("CAT"),  // Legacy Category (deprecated)
    COM("COM"),  // Component
    OPT("OPT"),  // ComponentOptional
    ITV("ITV"),  // ItemVariant
    ITC("ITC"),  // ItemVariantConfig
    LOC("LOC"),  // Location
    PRD("PRD"),  // Product
    COL("COL"),  // Collection
    CPR("CPR"),  // CollectionProduct
    FIL("FIL"),  // S3File
    VEN("VEN"),  // Vendor
    VPR("VPR"),  // VendorProduct
    DES("DES"),  // Designer
    DPR("DPR"),  // DesignProduct
    DST("DST"),  // DesignSaleTransaction
    ORD("ORD"),  // Order
    ORI("ORI"),  // OrderItem
    OPS("OPS"),  // OrderPaymentSchedule
    OSH("OSH"),  // OrderStatusHistory
    APT("APT"),  // Appointment
    CER("CER"),  // Certificate
    BLK("BLK"),  // BlockedSlot
    REC("REC"),  // StockReconciliationRecord
    RAK("RAK"),  // WarehouseRack
    SLT("SLT"),  // WarehouseSlot
    INP("INP"),  // InventoryPosition
    IPH("IPH"), // InventoryPositionHistory

    // PODs (Point of Display) System
    PTN("PTN"),  // PodPartner
    POD("POD"),  // Pod
    PQR("PQR"),  // PodQrCode
    PQS("PQS"),  // PodQrScan
    PAT("PAT"),  // PodAttribution
    PUA("PUA"),  // PodUserAttribution
    PCM("PCM"),  // PodCommission
    PTX("PTX"),  // PaymentTransaction

    // Phygital POD (Franchise Partner) System
    PIV("PIV"),  // PartnerInventory
    IVM("IVM"),  // InventoryMovement
    WHO("WHO"),  // WholesaleOrder
    WOI("WOI"),  // WholesaleOrderItem
    PSL("PSL"),  // PartnerSale
    PSI("PSI"),  // PartnerSaleItem

    // Production Partner Collaboration System
    JTR("JTR"),  // JewelryTechnicalReport (JTRC)
    JMC("JMC"),  // JTRCMetalComponent
    JSC("JSC"),  // JTRCStoneComponent
    JLC("JLC"),  // JTRCLaborComponent
    PCP("PCP"),  // PartnerCapability

    // Workflow Engine
    WFT("WFT"),  // WorkflowTemplate
    WFS("WFS"),  // WorkflowStage
    PPL("PPL"),  // ProductionPlan
    POR("POR"),  // ProductionOrder
    POS("POS"),  // ProductionOrderStage

    // Label Printing System
    LBT("LBT"),  // LabelTemplate

    // RFID Label Printing System
    PJB("PJB"),  // PrintJob
    PJI("PJI"),  // PrintJobItem
    RFT("RFT"),  // RFIDTag
    RSL("RSL"),  // RFIDScanLog

    // Component Tracking System
    HOL("HOL");  // HandoffOwnershipLog

    private final String prefix;

    EntityPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String generateId(Long sequenceValue) {
        return String.format("%s%06d", prefix, sequenceValue);
    }
}
