package com.turnosmart.turnosmart_backend.dto;

import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 12)
    private String dni;

    @Email(message = "Correo electrónico inválido")
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime time;

    @NotNull(message = "Seleccione el tipo de trámite")
    private Long procedureTypeId;

    // NULL = cualquier abogado disponible
    private Long lawyerId;

    private String notes;
}