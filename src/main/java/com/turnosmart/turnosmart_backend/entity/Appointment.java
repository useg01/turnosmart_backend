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

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    private String clientDni;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status; // SOLICITADO, REVISION, REGULARIZAR, CONFORME, etc.

    private String priority = "Normal"; // Para el semáforo del Dashboard (Alta, Normal, Baja)

    @Column(columnDefinition = "TEXT")
    private String notes; // Aquí el abogado guarda los comentarios de "Por qué regularizar"

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Relación con el historial de cambios de estado
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentLog> logs = new ArrayList<>();

    // Relación con los archivos físicos subidos (PDF, imágenes)
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentDocument> documents = new ArrayList<>();

    // Ciclos de vida para fechas automáticas
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

    // Método helper para añadir logs fácilmente
    public void addLog(AppointmentLog log) {
        logs.add(log);
        log.setAppointment(this);
    }
}