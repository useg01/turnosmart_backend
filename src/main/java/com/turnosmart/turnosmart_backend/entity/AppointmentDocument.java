package com.turnosmart.turnosmart_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "appointment_documents")
@Getter @Setter @NoArgsConstructor
public class AppointmentDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private String fileName;
    private String fileType;
    private String fileUrl;
}