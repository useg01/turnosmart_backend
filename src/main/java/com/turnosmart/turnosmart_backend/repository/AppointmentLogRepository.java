package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.AppointmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentLogRepository extends JpaRepository<AppointmentLog, Long> {

    List<AppointmentLog> findByAppointment_IdOrderByChangedAtDesc(Long appointmentId);

}