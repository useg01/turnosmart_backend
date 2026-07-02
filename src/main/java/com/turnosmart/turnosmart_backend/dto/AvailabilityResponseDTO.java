package com.turnosmart.turnosmart_backend.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityResponseDTO {

    private LocalDate date;

    private String dayStatus;

    private List<String> availableSlots;
}