package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TurnoRestController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping("/api/turnos/eventos")
    public List<Map<String, Object>> obtenerEventos() {
        List<Appointment> appointments = appointmentRepository.findAll();

        List<Map<String, Object>> eventos = appointments.stream().map(app -> {
            Map<String, Object> evento = new HashMap<>();
            evento.put("id", app.getId().toString());
            evento.put("title", app.getClient().getFirstName() + " - " + app.getProcedureType().getName());
            evento.put("start", app.getAppointmentDate().toString() + "T" + app.getAppointmentTime().toString());
            evento.put("color", obtenerColorSegunEstado(app.getStatus().name()));
            return evento;
        }).collect(Collectors.toList());

        java.util.Collections.reverse(eventos);
        return eventos;
    }

    private String obtenerColorSegunEstado(String status) {
        return switch (status) {
            case "CONFORME" -> "#10b981";
            case "REGULARIZAR" -> "#ef4444";
            case "REVISION" -> "#f59e0b";
            default -> "#6366f1";
        };
    }
}