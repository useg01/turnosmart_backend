package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;
import com.turnosmart.turnosmart_backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AppointmentRepository appointmentRepo;

    public Map<String, Object> getDashboardMetrics(String filter) {
        LocalDateTime start = getStartDate(filter);
        LocalDateTime end = LocalDateTime.now();

        long total = appointmentRepo.countByCreatedAtBetween(start, end);

        long enRevision = appointmentRepo.countByStatusAndCreatedAtBetween(AppointmentStatus.REVISION, start, end);

        long listos = appointmentRepo.countByStatusAndCreatedAtBetween(AppointmentStatus.LISTO_FIRMA, start, end);

        long regularizar = appointmentRepo.countByStatusAndCreatedAtBetween(AppointmentStatus.REGULARIZAR, start, end);
        double tasaReg = total > 0 ? (double) regularizar / total * 100 : 0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total", total);
        metrics.put("enCurso", enRevision);
        metrics.put("listos", listos);
        metrics.put("tasaReg", String.format("%.1f%%", tasaReg));

        return metrics;
    }

    private LocalDateTime getStartDate(String filter) {
        LocalDateTime ahora = LocalDateTime.now();
        return switch (filter.toLowerCase()) {
            case "dia" -> ahora.with(LocalTime.MIN);
            case "semana" -> ahora.minusWeeks(1);
            case "mes" -> ahora.minusMonths(1);
            case "año" -> ahora.minusYears(1);
            default -> ahora.minusMonths(1);
        };
    }
}