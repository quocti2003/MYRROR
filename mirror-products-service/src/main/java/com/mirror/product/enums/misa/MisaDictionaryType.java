package com.mirror.product.enums.misa;

/**
 * Dictionary type mappings for MISA AMIS ACT Open API.
 *
 * IMPORTANT: The save_dictionary and get_dictionary endpoints use DIFFERENT type numbering!
 * Use {@link SaveDictionaryType} for save_dictionary (writing) and
 * {@link GetDictionaryType} for get_dictionary (reading).
 */
public class MisaDictionaryType {

    private MisaDictionaryType() {}

    /**
     * Type values for POST /apir/sync/actopen/save_dictionary
     * Used when creating/updating dictionary entries in MISA.
     */
    public enum SaveDictionaryType {
        ACCOUNT_OBJECT(1, "Đối tượng (Customer/Vendor)"),
        ACCOUNT_OBJECT_GROUP(2, "Nhóm đối tượng"),
        INVENTORY_ITEM(3, "Vật tư hàng hóa (Inventory Item)"),
        INVENTORY_CATEGORY(4, "Nhóm vật tư hàng hóa"),
        STOCK(5, "Kho (Warehouse)"),
        UNIT(6, "Đơn vị tính (Unit of Measure)"),
        BANK_ACCOUNT(7, "Tài khoản ngân hàng"),
        BANK(8, "Ngân hàng"),
        EXPENSE_ITEM(9, "Khoản mục chi phí"),
        BUDGET_ITEM(10, "Mục thu chi"),
        JOB(12, "Đối tượng THCP (Job/Cost Object)");

        private final int value;
        private final String description;

        SaveDictionaryType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() { return value; }
        public String getDescription() { return description; }
    }

    /**
     * Type values for POST /apir/sync/actopen/get_dictionary
     * Used when reading/fetching dictionary entries from MISA.
     *
     * Note: These values are DIFFERENT from SaveDictionaryType!
     * For example, warehouses are type 5 for saving but type 3 for reading.
     */
    public enum GetDictionaryType {
        ACCOUNT_OBJECT(1, "Đối tượng (Customer/Vendor)"),
        INVENTORY_ITEM(2, "Vật tư hàng hóa (Inventory Item)"),
        STOCK(3, "Kho (Warehouse)"),
        UNIT(4, "Đơn vị tính (Unit of Measure)"),
        ACCOUNT(5, "Tài khoản kế toán (Accounting Account)"),
        BRANCH(6, "Chi nhánh (Organization Unit)");

        private final int value;
        private final String description;

        GetDictionaryType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() { return value; }
        public String getDescription() { return description; }
    }
}
