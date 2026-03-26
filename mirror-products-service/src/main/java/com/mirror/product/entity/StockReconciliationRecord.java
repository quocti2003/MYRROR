package com.mirror.product.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.mirror.product.util.SequenceIdGenerator;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.config.ApplicationContextProvider;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for storing stock reconciliation records.
 * Each record represents a single stock-take session with comparison
 * between physical counts and MISA system counts.
 */
@Entity
@Table(name = "stock_reconciliation_records", indexes = {
    @Index(name = "idx_recon_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_recon_created_by", columnList = "created_by"),
    @Index(name = "idx_recon_date", columnList = "reconciliation_date")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockReconciliationRecord extends BaseEntity {

    @Column(name = "document_number", nullable = false, unique = true)
    private String documentNumber;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDateTime reconciliationDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    // Summary statistics as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary", columnDefinition = "jsonb")
    private Map<String, Object> summary;

    // Full items data as JSON string
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_data", columnDefinition = "jsonb")
    private String itemsData;

    // R2 file storage info
    @Column(name = "report_file_key")
    private String reportFileKey;

    @Column(name = "report_file_name")
    private String reportFileName;

    @Column(name = "report_file_url")
    private String reportFileUrl;

    @Column(name = "notes", length = 2000)
    private String notes;

    // Statistics for quick queries
    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "matched_items")
    private Integer matchedItems;

    @Column(name = "missing_items")
    private Integer missingItems;

    @Column(name = "excess_items")
    private Integer excessItems;

    @Column(name = "not_in_system_items")
    private Integer notInSystemItems;

    @PrePersist
    public void generateId() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.REC));
        }
    }
}
