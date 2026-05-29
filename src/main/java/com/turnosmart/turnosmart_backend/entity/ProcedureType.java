package com.turnosmart.turnosmart_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "procedure_types")
@Getter @Setter @NoArgsConstructor
public class ProcedureType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Lob
    private String description;

    private Double price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 30;

    @Column(nullable = false)
    private Boolean active = true;
}
