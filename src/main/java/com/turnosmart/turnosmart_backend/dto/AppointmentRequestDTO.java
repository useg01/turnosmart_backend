package com.turnosmart.turnosmart_backend.dto;

import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {

    private String representationType;
    private String identifier;
    private String businessName;
    private String repDni;
    private String repNombres;
    private String repApellidos;
    private String repFechaNac;
    private String repEstadoCivil;
    private String repNacionalidad;
    private String repCorreo;
    private String repTelefono;
    private String repDireccion;

    //datos del apoderado
    private String apoDni;
    private String apoNombres;
    private String apoApellidos;
    private String apoCorreo;
    private String apoTelefono;
    private String apoDireccion;

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

    private Long lawyerId;

    private String notes;
}