package com.mirror.product.entity;

import com.mirror.product.config.ApplicationContextProvider;
import com.mirror.product.enums.AppointmentStatus;
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

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_date", columnList = "appointment_date"),
    @Index(name = "idx_appointment_venue_date", columnList = "venue_id, appointment_date"),
    @Index(name = "idx_appointment_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment extends BaseEntity {

    @Column(name = "customer_title", length = 20)
    private String customerTitle;

    @Column(name = "customer_first_name", nullable = false, length = 100)
    private String customerFirstName;

    @Column(name = "customer_last_name", nullable = false, length = 100)
    private String customerLastName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 50)
    private String customerPhone;

    @Column(name = "venue_id", nullable = false, length = 30)
    private String venueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Location venue;

    @Column(name = "service", length = 100)
    private String service;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "language", length = 20)
    @Default
    private String language = "en";

    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "staff_notes", columnDefinition = "TEXT")
    private String staffNotes;

    @PrePersist
    public void prePersist() {
        if (getId() == null || getId().isEmpty()) {
            SequenceIdGenerator sequenceIdGenerator = ApplicationContextProvider.getBean(SequenceIdGenerator.class);
            setId(sequenceIdGenerator.generateId(EntityPrefix.APT));
        }
    }
}
