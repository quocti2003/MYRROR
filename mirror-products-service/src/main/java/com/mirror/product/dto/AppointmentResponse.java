package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirror.product.enums.AppointmentStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentResponse {

    private String id;

    private String customerTitle;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;

    private String venueId;
    private String venueName;
    private String venueAddress;
    private String venueCity;

    private String service;

    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    private String language;
    private String preferences;

    private AppointmentStatus status;
    private String cancellationReason;
    private String staffNotes;

    private Instant createdAt;
    private Instant updatedAt;
}
