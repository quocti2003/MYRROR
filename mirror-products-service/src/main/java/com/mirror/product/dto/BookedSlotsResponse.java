package com.mirror.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookedSlotsResponse {

    private LocalDate date;
    private String venueId;
    private List<LocalTime> bookedSlots;
    private List<LocalTime> availableSlots;
}
