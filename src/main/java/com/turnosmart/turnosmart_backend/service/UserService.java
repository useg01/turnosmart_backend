package com.turnosmart.turnosmart_backend.service;

import com.turnosmart.turnosmart_backend.entity.Role;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.RoleRepository;
import com.turnosmart.turnosmart_backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAllActive() {
        return userRepository.findAll().stream()
                .filter(User::getEnabled)
                .toList();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public User findByDni(String dni) {
        return userRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Usuario con DNI " + dni + " no encontrado"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public User register(User user) {
        if (userRepository.existsByDni(user.getDni())) {
            throw new RuntimeException("El DNI ya se encuentra registrado");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        // CORREGIDO: Guarda el hash de forma consistente usando setPasswordHash
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
        }

        Role clienteRole = roleRepository.findByName("ROLE_CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error: El rol ROLE_CLIENTE no existe en la DB"));

        user.getRoles().add(clienteRole);
        return userRepository.save(user);
    }

    public User update(Long id, User updatedUser) {
        User user = findById(id);

        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setPhone(updatedUser.getPhone());
        user.setEnabled(updatedUser.getEnabled());

        return userRepository.save(user);
    }

    public User deactivate(Long id) {
        User user = findById(id);
        user.setEnabled(false);
        return userRepository.save(user);
    }

    public User activate(Long id) {
        User user = findById(id);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByDni(String dni) {
        return userRepository.existsByDni(dni);
    }
}