package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.Lawyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LawyerRepository extends JpaRepository<Lawyer, Long> {
    List<Lawyer> findByActiveTrue();
    boolean existsByColegiatura(String colegiatura);

    // Algoritmo de balanceo: Selecciona al abogado activo con menos citas/expedientes asociados
    @Query("SELECT l FROM Lawyer l " +
            "LEFT JOIN Appointment a ON a.lawyer = l " +
            "WHERE l.active = true " +
            "GROUP BY l.id " +
            "ORDER BY COUNT(a.id) ASC, l.id ASC")
    List<Lawyer> findLawyersOrderByLoad();
}