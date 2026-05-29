package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.ProcedureType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureTypeRepository extends JpaRepository<ProcedureType, Long> {
}