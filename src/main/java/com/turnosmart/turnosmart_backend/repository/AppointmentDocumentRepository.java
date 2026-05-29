package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.AppointmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentDocumentRepository extends JpaRepository<AppointmentDocument, Long> {
    List<AppointmentDocument> findByAppointmentId(Long appointmentId);
}