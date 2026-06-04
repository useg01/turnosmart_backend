package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.Lawyer;
import com.turnosmart.turnosmart_backend.entity.Role;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.LawyerRepository;
import com.turnosmart.turnosmart_backend.repository.UserRepository;
import com.turnosmart.turnosmart_backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.turnosmart.turnosmart_backend.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LawyerService {

    private final LawyerRepository lawyerRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

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

        User user = lawyer.getUser();
        if (user == null) {
            throw new BusinessException("El abogado debe tener una cuenta de usuario asociada.");
        }

        if (lawyer.getId() == null) {
            if (userRepo.existsByEmail(user.getEmail())) {
                throw new BusinessException("El correo corporativo ya se encuentra registrado.");
            }

            if (userRepo.existsByDni(user.getDni())) {
                throw new BusinessException("El DNI del profesional ya se encuentra registrado.");
            }

            if (lawyerRepo.existsByColegiatura(lawyer.getColegiatura())) {
                throw new BusinessException("El número de colegiatura ingresado ya se encuentra registrado.");
            }
        }

        Role abogadoRole = roleRepo.findByName("ROLE_NOTARIO")
                .orElseThrow(() -> new BusinessException("Error crítico: El rol ROLE_NOTARIO no existe en la base de datos."));
        user.setSingleRole(abogadoRole);

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