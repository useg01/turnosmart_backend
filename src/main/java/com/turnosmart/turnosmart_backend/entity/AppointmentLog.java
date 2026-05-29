package com.turnosmart.turnosmart_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "appointment_logs")
@Getter @Setter @NoArgsConstructor
public class AppointmentLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Lob
    private String comments;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}