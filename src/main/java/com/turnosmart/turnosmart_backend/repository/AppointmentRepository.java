package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedAtBetween(AppointmentStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.client " +
            "JOIN FETCH a.procedureType " +
            "LEFT JOIN FETCH a.lawyer")
    List<Appointment> findAllWithDetails();

    long countByLawyerIdAndAppointmentDate(Long lawyerId, LocalDate date);

    boolean existsByLawyerIdAndAppointmentDateAndAppointmentTime(Long lawyerId, LocalDate date, LocalTime time);

    @Query("SELECT a.appointmentDate FROM Appointment a " +
            "WHERE a.lawyer.id = :lawyerId " +
            "GROUP BY a.appointmentDate HAVING COUNT(a) >= 6")
    List<LocalDate> findFullDaysByLawyer(@Param("lawyerId") Long lawyerId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.procedureType WHERE a.client.id = :clientId")
    List<Appointment> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.client " +
            "JOIN FETCH a.procedureType " +
            "WHERE a.lawyer.user.id = :userId")
    List<Appointment> findByLawyerId(@Param("userId") Long userId);

    Optional<Appointment> findByTicketNumber(String ticketNumber);
}