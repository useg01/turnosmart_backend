package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.Lawyer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LawyerRepository extends JpaRepository<Lawyer, Long> {
    List<Lawyer> findByActiveTrue();

    // CORRECCIÓN PARA EL CUS03 (Excepción E1)
    boolean existsByColegiatura(String colegiatura);
}
