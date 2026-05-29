package com.turnosmart.turnosmart_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class AppointmentResponseDTO {

    private Long id;
    private String ticketNumber;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
}