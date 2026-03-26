package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.BlockType;
import com.mirror.product.enums.EntityPrefix;
import com.mirror.product.util.SequenceIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.Builder.Default;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entity representing blocked time slots that cannot be booked by customers.
 * Admins can block slots for lunch breaks, meetings, holidays, etc.
 */
@Entity
@Table(name = "blocked_slots", indexes = {
    @Index(name = "idx_blocked_slot_date", columnList = "block_date"),
    @Index(name = "idx_blocked_slot_venue_date", columnList = "venue_id, block_date"),
    @Index(name = "idx_blocked_slot_type", columnList = "block_type")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSlot extends BaseEntity {

    @Column(name = "venue_id", length = 30)
    private String venueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Location venue;

    @Column(name = "block_date", nullable = false)
    private LocalDate blockDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "is_full_day", nullable = false)
    @Default
    private Boolean isFullDay = false;

    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    @Default
    private BlockType blockType = BlockType.SINGLE;

    @Column(name = "recurring_end_date")
    private LocalDate recurringEndDate;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.BLK));
        }
    }
}
