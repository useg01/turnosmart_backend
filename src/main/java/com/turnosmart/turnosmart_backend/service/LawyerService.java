package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.Lawyer;
import com.turnosmart.turnosmart_backend.repository.LawyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.turnosmart.turnosmart_backend.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LawyerService {

    private final LawyerRepository lawyerRepo;

    @Transactional(readOnly = true)
    public List<Lawyer> findAll() {
        return lawyerRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Lawyer findById(Long id) {
        if (id == null) {
            throw new BusinessException("El ID del abogado no puede ser nulo.");
        }
        return lawyerRepo.findById(id)
                .orElseThrow(() -> new BusinessException("El abogado con ID " + id + " no existe."));
    }

    @Transactional
    public Lawyer save(Lawyer lawyer) {
        if (lawyer == null) {
            throw new BusinessException("No se puede guardar un objeto nulo.");
        }
        return lawyerRepo.save(lawyer);
    }


    @Transactional
    public void delete(Long id) {
        if (id == null || !lawyerRepo.existsById(id)) {
            throw new BusinessException("No se puede eliminar: El abogado no existe o el ID es nulo.");
        }
        lawyerRepo.deleteById(id);
    }
}