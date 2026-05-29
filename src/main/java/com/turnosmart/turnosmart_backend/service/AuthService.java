package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.UserRepository; // Importante
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;

    public void register(User user) {
        userService.register(user);
    }

    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    // Verifica que en la clase User el campo se llame exactamente passwordHash
                    return user.getPassword().equals(password);
                })
                .orElse(false);
    }
}