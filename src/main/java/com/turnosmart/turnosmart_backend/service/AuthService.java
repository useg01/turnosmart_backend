package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.Role;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.RoleRepository;
import com.turnosmart.turnosmart_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Inyectamos el repositorio de roles

    public void register(User user) {
        // =========================================================
        // REGLAS DE NEGOCIO (Validaciones del Core)
        // =========================================================

        // RN-01: El correo electrónico debe ser único
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El correo electrónico ya se encuentra registrado.");
        }

        // Validación extra de seguridad: El DNI debe ser único
        if (userRepository.existsByDni(user.getDni())) {
            throw new RuntimeException("El DNI ingresado ya se encuentra registrado.");
        }

        // RN-02: La contraseña debe contener mínimo 8 caracteres
        if (user.getPassword() == null || user.getPassword().trim().length() < 8) {
            throw new RuntimeException("La contraseña es muy corta. Debe contener un mínimo de 8 caracteres.");
        }

        // =========================================================
        // ASIGNACIÓN DE ROL POR DEFECTO
        // =========================================================
        Role defaultRole = roleRepository.findByName("ROLE_CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error crítico: El rol ROLE_CLIENTE no existe en la base de datos."));

        user.setSingleRole(defaultRole);

        // Continuamos con el guardado normal en la capa de persistencia
        userService.register(user);
    }

    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }
}