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

    /**
     * Calcula métricas analíticas para el Dashboard basadas en un filtro temporal.
     * @param filter Filtro de tiempo (dia, semana, mes, año).
     * @return Mapa con métricas calculadas.
     */
    public Map<String, Object> getDashboardMetrics(String filter) {
        LocalDateTime start = getStartDate(filter);
        LocalDateTime end = LocalDateTime.now();

        // 1. Total de trámites creados en este periodo
        long total = appointmentRepo.countByCreatedAtBetween(start, end);

        // 2. Trámites en Revisión (Pendientes)
        long enRevision = appointmentRepo.countByStatusAndCreatedAtBetween(AppointmentStatus.REVISION, start, end);

        // 3. Trámites Listos para firma (Eficiencia)
        long listos = appointmentRepo.countByStatusAndCreatedAtBetween(AppointmentStatus.LISTO_FIRMA, start, end);

        // 4. Tasa de Regularización (Trámites observados)
        long regularizar = appointmentRepo.countByStatusAndCreatedAtBetween(AppointmentStatus.REGULARIZAR, start, end);
        double tasaReg = total > 0 ? (double) regularizar / total * 100 : 0;

        // Empaquetado de métricas para la vista
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