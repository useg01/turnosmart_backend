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
    private final RoleRepository roleRepository;

    public void register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El correo electrónico ya se encuentra registrado.");
        }

        if (userRepository.existsByDni(user.getDni())) {
            throw new RuntimeException("El DNI ingresado ya se encuentra registrado.");
        }
        if (user.getPassword() == null || user.getPassword().trim().length() < 8) {
            throw new RuntimeException("La contraseña es muy corta. Debe contener un mínimo de 8 caracteres.");
        }

        Role defaultRole = roleRepository.findByName("ROLE_CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error crítico: El rol ROLE_CLIENTE no existe en la base de datos."));

        user.setSingleRole(defaultRole);

        userService.register(user);
    }

    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }
}