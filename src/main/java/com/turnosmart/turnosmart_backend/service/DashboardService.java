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

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboardData(String filter) {
        Map<String, Object> metrics = analyticsService.getDashboardMetrics(filter);

        List<Appointment> recentAppointments = appointmentRepository.findAllWithDetails();

        return Map.of(
                "metrics", metrics,
                "tramites", recentAppointments,
                "currentFilter", filter
        );
    }
}