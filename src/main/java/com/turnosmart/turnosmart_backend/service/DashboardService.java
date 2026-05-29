package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AnalyticsService analyticsService;
    private final AppointmentRepository appointmentRepository;

    /**
     * Reúne toda la información necesaria para el panel de administración.
     * Incluye métricas calculadas y la lista de trámites detallada.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboardData(String filter) {
        // 1. Obtenemos las métricas (Totales, Revisión, Tasa de Reg, etc.)
        Map<String, Object> metrics = analyticsService.getDashboardMetrics(filter);

        // 2. Obtenemos la lista de trámites con JOIN FETCH para evitar errores de carga
        List<Appointment> recentAppointments = appointmentRepository.findAllWithDetails();

        // 3. Retornamos todo en un mapa para el controlador
        return Map.of(
                "metrics", metrics,
                "tramites", recentAppointments,
                "currentFilter", filter
        );
    }
}