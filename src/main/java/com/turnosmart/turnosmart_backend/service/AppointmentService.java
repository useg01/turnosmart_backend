package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.dto.AppointmentRequestDTO;
import com.turnosmart.turnosmart_backend.dto.AppointmentResponseDTO;
import com.turnosmart.turnosmart_backend.entity.*;
import com.turnosmart.turnosmart_backend.repository.*;
import com.turnosmart.turnosmart_backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final LawyerRepository lawyerRepo;
    private final UserRepository userRepo;
    private final ProcedureTypeRepository procedureTypeRepo;
    private final AppointmentLogRepository logRepo;

    // =========================================================
    // MÉTODOS PARA DASHBOARD / ADMIN
    // =========================================================

    public long count() {
        return appointmentRepo.count();
    }

    public List<Appointment> findAll() {
        return appointmentRepo.findAllWithDetails();
    }

    // =========================================================
    // GESTIÓN DE DISPONIBILIDAD (REGLA DE LOS 6 TURNOS)
    // =========================================================

    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(LocalDate date, Long lawyerId) {
        long dailyCount = appointmentRepo.countByLawyerIdAndAppointmentDate(lawyerId, date);
        if (dailyCount >= 6) return List.of();

        List<LocalTime> allSlots = List.of(
                LocalTime.of(9, 0), LocalTime.of(9, 30),
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(12, 0), LocalTime.of(12, 30)
        );

        return allSlots.stream()
                .filter(time -> !appointmentRepo.existsByLawyerIdAndAppointmentDateAndAppointmentTime(lawyerId, date, time))
                .map(LocalTime::toString)
                .collect(Collectors.toList());
    }

    public List<LocalDate> findDiasOcupados(Long lawyerId) {
        return appointmentRepo.findFullDaysByLawyer(lawyerId);
    }

    // =========================================================
    // OPERACIONES DE CITA (CREATE / UPDATE)
    // =========================================================

    @Transactional
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto, Long clientUserId) {
        Lawyer lawyer = lawyerRepo.findById(dto.getLawyerId())
                .orElseThrow(() -> new BusinessException("El abogado seleccionado no existe."));

        //SE ELIMINÓ: La validación de los 6 cupos diarios porque el cliente ya no elige fecha.

        User client = userRepo.findById(clientUserId)
                .orElseThrow(() -> new BusinessException("Usuario cliente no encontrado."));

        ProcedureType procedure = procedureTypeRepo.findById(dto.getProcedureTypeId())
                .orElseThrow(() -> new BusinessException("Tipo de trámite no válido."));

        Appointment app = new Appointment();
        app.setTicketNumber("TS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        app.setClient(client);
        app.setLawyer(lawyer);
        app.setProcedureType(procedure);

        // Aceptarán valores nulos temporalmente hasta que el abogado los programe
        app.setAppointmentDate(dto.getDate());
        app.setAppointmentTime(dto.getTime());

        // Mantenemos el estado inicial en REVISION para que el staff lo evalúe
        app.setStatus(AppointmentStatus.REVISION);

        Appointment saved = appointmentRepo.save(app);

        registrarLog(saved, "N/A", "REVISION", "Trámite registrado correctamente sin fecha programada.", clientUserId);

        return new AppointmentResponseDTO(saved.getId(), saved.getTicketNumber(),
                saved.getAppointmentDate(), saved.getAppointmentTime(), saved.getStatus().name());
    }

    @Transactional
    public void changeStatus(Long appId, AppointmentStatus nuevoEstado, String comentario, Long actorId) {
        Appointment app = appointmentRepo.findById(appId)
                .orElseThrow(() -> new BusinessException("La cita no existe."));

        String estadoAnterior = app.getStatus().name();
        app.setStatus(nuevoEstado);
        app.setNotes(comentario);

        appointmentRepo.save(app);

        registrarLog(app, estadoAnterior, nuevoEstado.name(), comentario, actorId);
    }

    // =========================================================
    // CONSULTAS Y DOCUMENTOS
    // =========================================================

    public List<Appointment> findByClient(Long clientId) {
        return appointmentRepo.findByClientId(clientId);
    }

    public List<Appointment> findByLawyer(Long lawyerId) {
        return appointmentRepo.findByLawyerId(lawyerId);
    }

    public Appointment findByTicket(String ticket) {
        return appointmentRepo.findByTicketNumber(ticket)
                .orElseThrow(() -> new BusinessException("No se encontró el trámite con ticket: " + ticket));
    }

    @Transactional
    public void uploadDocuments(Long appointmentId, List<MultipartFile> files, Long userId) {
        Appointment app = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Cita no encontrada para subir archivos."));

        // CORRECCIÓN: Se pasa el userId para la auditoría del log
        registrarLog(app, app.getStatus().name(), app.getStatus().name(), "Se cargaron nuevos documentos adjuntos.", userId);
    }

    // =========================================================
    // MÉTODOS PRIVADOS
    // =========================================================

    private void registrarLog(Appointment app, String oldS, String newS, String comment, Long userId) {
        // 1. Buscamos el usuario por su ID
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado para el log"));

        AppointmentLog log = new AppointmentLog();
        log.setAppointment(app);
        log.setOldStatus(oldS);
        log.setNewStatus(newS);
        log.setComments(comment);

        // 2. Ahora sí pasamos el objeto 'user', no el ID
        log.setChangedBy(user);

        log.setChangedAt(LocalDateTime.now());
        logRepo.save(log);
    }
}