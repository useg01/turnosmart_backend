package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.Role;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.RoleRepository;
import com.turnosmart.turnosmart_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

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

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPasswordHash(encodedPassword);

        userService.register(user);
    }

    public boolean authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (user.getAccountLocked() != null && user.getAccountLocked()) {
            throw new RuntimeException("La cuenta se encuentra bloqueada por superar el límite de 3 intentos fallidos.");
        }

        if (passwordEncoder.matches(password.trim(), user.getPasswordHash().trim())) {
            if (user.getFailedAttempts() == null || user.getFailedAttempts() > 0) {
                user.setFailedAttempts(0);
                userRepository.save(user);
            }
            return true;
        } else {
            int currentAttempts = (user.getFailedAttempts() != null) ? user.getFailedAttempts() : 0;
            currentAttempts++;
            user.setFailedAttempts(currentAttempts);

            if (currentAttempts >= 3) {
                user.setAccountLocked(true);
                userRepository.save(user);
                throw new RuntimeException("Contraseña incorrecta. La cuenta ha sido bloqueada tras 3 intentos fallidos.");
            }

            userRepository.save(user);
            return false;
        }
    }
}