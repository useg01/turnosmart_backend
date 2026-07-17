package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a JOIN FETCH a.client JOIN FETCH a.lawyer l JOIN FETCH l.user JOIN FETCH a.procedureType")
    List<Appointment> findAllWithDetails();

    @Query("SELECT a FROM Appointment a JOIN FETCH a.client JOIN FETCH a.lawyer l JOIN FETCH l.user JOIN FETCH a.procedureType " +
            "WHERE a.createdAt >= :desde AND a.createdAt <= :hasta " +
            "ORDER BY a.createdAt DESC")
    List<Appointment> findByCreatedAtBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    List<Appointment> findByClientId(Long clientId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.client JOIN FETCH a.lawyer l JOIN FETCH l.user JOIN FETCH a.procedureType WHERE l.id = :lawyerId")
    List<Appointment> findByLawyerId(@Param("lawyerId") Long lawyerId);

    long countByLawyerIdAndAppointmentDate(Long lawyerId, LocalDate date);

    boolean existsByLawyerIdAndAppointmentDateAndAppointmentTime(Long lawyerId, LocalDate date, LocalTime time);

    @Query("SELECT a.appointmentDate FROM Appointment a WHERE a.lawyer.id = :lawyerId " +
            "GROUP BY a.appointmentDate HAVING COUNT(a) >= 6")
    List<LocalDate> findFullDaysByLawyer(@Param("lawyerId") Long lawyerId);

    Optional<Appointment> findByTicketNumber(String ticketNumber);

    @Query("SELECT a.lawyer.id, COUNT(a) FROM Appointment a GROUP BY a.lawyer.id")
    List<Object[]> countByLawyer();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedAtBetween(AppointmentStatus status, LocalDateTime start, LocalDateTime end);
}