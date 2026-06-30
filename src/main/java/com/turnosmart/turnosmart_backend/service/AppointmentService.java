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
    private final AppointmentDocumentRepository documentRepo;
    private final FileService fileService;

    private static final java.util.Set<String> CODIGOS_OPERACION_VALIDOS = java.util.Set.of(
            "012345", "054823", "112233", "998877", "445566"
    );

    public long count() {
        return appointmentRepo.count();
    }

    public List<Appointment> findAll() {
        return appointmentRepo.findAllWithDetails();
    }

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

    @Transactional
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto, Long clientUserId) {

        String codigoIngresado = dto.getOperationNumber() != null ? dto.getOperationNumber().trim() : "";
        if (codigoIngresado.isEmpty() || !CODIGOS_OPERACION_VALIDOS.contains(codigoIngresado)) {
            throw new BusinessException("El número de operación ingresado no es válido. Verifique el código de su comprobante de pago e intente nuevamente.");
        }

        List<Lawyer> availableLawyers = lawyerRepo.findLawyersOrderByLoad();
        if (availableLawyers.isEmpty()) {
            throw new BusinessException("No existen abogados activos registrados en el sistema para la asignación.");
        }
        Lawyer automaticallyAssignedLawyer = availableLawyers.get(0);

        User client = userRepo.findById(clientUserId)
                .orElseThrow(() -> new BusinessException("Usuario cliente no encontrado."));

        ProcedureType procedure = procedureTypeRepo.findById(dto.getProcedureTypeId())
                .orElseThrow(() -> new BusinessException("Tipo de trámite no válido."));

        Appointment app = new Appointment();
        app.setTicketNumber("TS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        app.setClient(client);
        app.setLawyer(automaticallyAssignedLawyer);
        app.setProcedureType(procedure);
        app.setClientDni(client.getDni());

        // Mapeo de campos de cabecera de representación
        app.setRepresentationType(dto.getRepresentationType());
        app.setIdentifier(dto.getIdentifier());
        app.setBusinessName(dto.getBusinessName());
        app.setStatus(AppointmentStatus.PENDIENTE_EVALUACION);

        // Datos de pago, ya validados arriba
        app.setPaymentMethod(dto.getPaymentMethod());
        app.setOperationNumber(codigoIngresado);
        app.setIsPaid(true);

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append("           EXPEDIENTE DETALLADO DE SOLICITUD            \n");
        sb.append("========================================================\n\n");

        sb.append("[REQUERIMIENTO PRINCIPAL]\n");
        sb.append("· Descripción/Notas del Cliente: ").append(dto.getNotes() != null ? dto.getNotes() : "No especificado").append("\n\n");

        // Si se llenó el módulo de representación legal (Persona Jurídica o Natural)
        if (dto.getRepresentationType() != null && !dto.getRepresentationType().trim().isEmpty()) {
            sb.append("[MÓDULO DE REPRESENTACIÓN LEGAL]\n");
            sb.append("· Tipo de Persona: ").append(dto.getRepresentationType()).append("\n");
            sb.append("· Identificador / RUC: ").append(dto.getIdentifier() != null ? dto.getIdentifier() : "N/A").append("\n");
            sb.append("· Razón Social / Empresa: ").append(dto.getBusinessName() != null ? dto.getBusinessName() : "N/A").append("\n\n");
        }

        if (dto.getRepDni() != null && !dto.getRepDni().trim().isEmpty()) {
            sb.append("[DATOS DEL REPRESENTADO (OTORGA EN EL PODER)]\n");
            sb.append("· DNI: ").append(dto.getRepDni()).append("\n");
            sb.append("· Nombres Completos: ").append(dto.getRepNombres()).append(" ").append(dto.getRepApellidos()).append("\n");
            sb.append("· Fecha de Nacimiento: ").append(dto.getRepFechaNac() != null ? dto.getRepFechaNac() : "No declarada").append("\n");
            sb.append("· Estado Civil: ").append(dto.getRepEstadoCivil() != null ? dto.getRepEstadoCivil() : "No declarado").append("\n");
            sb.append("· Nacionalidad: ").append(dto.getRepNacionalidad() != null ? dto.getRepNacionalidad() : "No declarada").append("\n");
            sb.append("· Correo: ").append(dto.getRepCorreo() != null ? dto.getRepCorreo() : "No declarado").append("\n");
            sb.append("· Teléfono: ").append(dto.getRepTelefono() != null ? dto.getRepTelefono() : "No registrado").append("\n");
            sb.append("· Dirección Completa: ").append(dto.getRepDireccion() != null ? dto.getRepDireccion() : "No registrada").append("\n\n");
        }

        if (dto.getApoDni() != null && !dto.getApoDni().trim().isEmpty()) {
            sb.append("[DATOS DEL APODERADO (RECIBE EL PODER)]\n");
            sb.append("· DNI: ").append(dto.getApoDni()).append("\n");
            sb.append("· Nombres Completos: ").append(dto.getApoNombres()).append(" ").append(dto.getApoApellidos()).append("\n");
            sb.append("· Correo Electrónico: ").append(dto.getApoCorreo() != null ? dto.getApoCorreo() : "No declarado").append("\n");
            sb.append("· Teléfono / Celular: ").append(dto.getApoTelefono() != null ? dto.getApoTelefono() : "No registrado").append("\n");
            sb.append("· Dirección de Residencia: ").append(dto.getApoDireccion() != null ? dto.getApoDireccion() : "No registrada").append("\n");
            sb.append("· Facultades: ").append(dto.getApoFacultades() != null ? dto.getApoFacultades() : "No se especificaron facultades").append("\n");

            sb.append("========================================================\n");
        }

        app.setClientNotes(sb.toString());

        Appointment saved = appointmentRepo.save(app);

        String logComment = "Trámite de Gestión Notarial iniciado por el cliente. Asignado automáticamente al Especialista: "
                + automaticallyAssignedLawyer.getUser().getFirstName() + " " + automaticallyAssignedLawyer.getUser().getLastName();

        registrarLog(saved, "N/A", saved.getStatus().name(), logComment, clientUserId);

        return new AppointmentResponseDTO(saved.getId(), saved.getTicketNumber(), null, null, saved.getStatus().name());
    }

    @Transactional
    public void changeStatus(Long appId, AppointmentStatus nuevoEstado, String comentario, Long actorId) {
        Appointment app = appointmentRepo.findById(appId)
                .orElseThrow(() -> new BusinessException("La cita no existe."));

        String estadoAnterior = app.getStatus().name();
        app.setStatus(nuevoEstado);
        app.setLawyerNotes(comentario);

        appointmentRepo.save(app);

        registrarLog(app, estadoAnterior, nuevoEstado.name(), comentario, actorId);
    }

    @Transactional
    public void subsanarTramite(Long appId, String clientObservation, Long clientUserId) {
        Appointment app = appointmentRepo.findById(appId)
                .orElseThrow(() -> new BusinessException("El trámite no existe."));

        if (app.getStatus() != AppointmentStatus.REGULARIZAR
                && app.getStatus() != AppointmentStatus.PROCESO_DETENIDO) {
            throw new BusinessException("Solo se puede subsanar un trámite que se encuentre en estado 'Por Regularizar'.");
        }

        String estadoAnterior = app.getStatus().name();

        app.setClientObservation(clientObservation);
        app.setStatus(AppointmentStatus.REVISION);

        appointmentRepo.save(app);

        registrarLog(app, estadoAnterior, AppointmentStatus.REVISION.name(),
                "Cliente envió subsanación: " + clientObservation, clientUserId);
    }

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
    public void uploadDocuments(Long appointmentId, MultipartFile fileDni, MultipartFile fileRecibo, Long userId) {
        Appointment app = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Cita no encontrada para subir archivos."));

        if (fileDni == null || fileDni.isEmpty() || fileRecibo == null || fileRecibo.isEmpty()) {
            throw new BusinessException("Debe adjuntar tanto el DNI como el Recibo de Agua o Luz en formato PDF.");
        }

        String nombreDni = fileService.save(fileDni, app.getTicketNumber() + "_DNI");
        AppointmentDocument docDni = new AppointmentDocument();
        docDni.setAppointment(app);
        docDni.setFileName(nombreDni);
        docDni.setFileType("DNI");
        docDni.setFileUrl("/uploads/tramites/" + nombreDni);
        documentRepo.save(docDni);

        String nombreRecibo = fileService.save(fileRecibo, app.getTicketNumber() + "_RECIBO");
        AppointmentDocument docRecibo = new AppointmentDocument();
        docRecibo.setAppointment(app);
        docRecibo.setFileName(nombreRecibo);
        docRecibo.setFileType("RECIBO_SERVICIO");
        docRecibo.setFileUrl("/uploads/tramites/" + nombreRecibo);
        documentRepo.save(docRecibo);

        String estadoAnterior = app.getStatus().name();
        app.setStatus(AppointmentStatus.DOCUMENTOS_ENVIADOS);
        appointmentRepo.save(app);

        registrarLog(app, estadoAnterior, AppointmentStatus.DOCUMENTOS_ENVIADOS.name(),
                "El cliente cargó/actualizó sus documentos: DNI y Recibo de Agua/Luz.", userId);
    }

    private void registrarLog(Appointment app, String oldS, String newS, String comment, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado para el log"));

        AppointmentLog log = new AppointmentLog();
        log.setAppointment(app);
        log.setOldStatus(oldS);
        log.setNewStatus(newS);
        log.setComments(comment);
        log.setChangedBy(user);
        log.setChangedAt(LocalDateTime.now());
        logRepo.save(log);
    }
}