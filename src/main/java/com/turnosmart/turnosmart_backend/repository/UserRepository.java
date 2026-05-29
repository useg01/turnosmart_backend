package com.turnosmart.turnosmart_backend.repository;

import com.turnosmart.turnosmart_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoles_Name(String roleName);

    Optional<User> findByDni(String dni);

    boolean existsByDni(String dni);

}