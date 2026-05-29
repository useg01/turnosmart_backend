package com.turnosmart.turnosmart_backend.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityResponseDTO {

    private LocalDate date;

    // AVAILABLE, PARTIAL, FULL, HOLIDAY, WEEKEND
    private String dayStatus;

    // Horarios disponibles
    private List<String> availableSlots;
}