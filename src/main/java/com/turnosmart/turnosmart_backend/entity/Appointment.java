package com.turnosmart.turnosmart_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_type_id", nullable = false)
    private ProcedureType procedureType;

    @Column(nullable = true)
    private LocalDate appointmentDate;

    @Column(nullable = true)
    private LocalTime appointmentTime;

    private String clientDni;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    private String priority = "Normal";

    private String representationType;

    private String identifier;

    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String clientNotes;

    @Column(columnDefinition = "TEXT")
    private String lawyerNotes;

    /**
     * Respuesta/observación que el cliente redacta al "Subsanar" un trámite
     * que el especialista marcó como REGULARIZAR / PROCESO_DETENIDO.
     * Se guarda separado de clientNotes para no mezclarse con el parser
     * de "Facultades Especiales Otorgadas" en las vistas del abogado.
     */
    @Column(columnDefinition = "TEXT")
    private String clientObservation;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentLog> logs = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentDocument> documents = new ArrayList<>();

    /**
     * Ruta del PDF de carta legal generado automáticamente al aprobar los documentos.
     * Sirve tanto al cliente (para descargarlo) como al abogado/admin para consulta.
     */
    @Column(columnDefinition = "TEXT")
    private String cartaGeneradaUrl;

    private String paymentMethod;
    private String operationNumber;
    private Boolean isPaid = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = AppointmentStatus.SOLICITADO;
        if (this.priority == null) this.priority = "Normal";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addLog(AppointmentLog log) {
        logs.add(log);
        log.setAppointment(this);
    }
}