package com.turnosmart.turnosmart_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity @Table(name = "lawyer_schedules")
@Getter @Setter @NoArgsConstructor
public class LawyerSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;   // 1=Lunes ... 7=Domingo

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_minutes", nullable = false)
    private Integer slotMinutes = 30;
}