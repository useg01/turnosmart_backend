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
        // Buscamos el rol de cliente directamente en la base de datos
        Role defaultRole = roleRepository.findByName("ROLE_CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error crítico: El rol ROLE_CLIENTE no existe en la base de datos."));

        // Le asignamos el rol al nuevo usuario usando su método helper
        user.setSingleRole(defaultRole);

        // Continuamos con el guardado normal
        userService.register(user);
    }

    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }
}