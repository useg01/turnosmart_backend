package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Role;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.AuthService;
import com.turnosmart.turnosmart_backend.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String loginManual(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {
        try {
            // 1. MODIFICACIÓN DE SEGURIDAD: Usamos la lógica centralizada de autenticación (valida hash y cuenta intentos)
            boolean isAuthenticated = authService.authenticate(email, password);

            if (isAuthenticated) {
                User user = userService.findByEmail(email);

                if (user.getRoles() == null || user.getRoles().isEmpty()) {
                    model.addAttribute("error", "El usuario no tiene un rol asignado en el sistema.");
                    return "login";
                }

                List<String> roleNames = user.getRoles().stream()
                        .map(role -> role.getName().toUpperCase())
                        .toList();

                String interfaz;

                if (roleNames.contains("ROLE_ADMIN") || roleNames.contains("ADMIN")) {
                    interfaz = "ADMIN";
                } else if (roleNames.contains("ROLE_NOTARIO") || roleNames.contains("NOTARIO") || roleNames.contains("ROLE_ABOGADO") || roleNames.contains("ABOGADO")) {
                    interfaz = "ABOGADO";
                } else {
                    interfaz = "CLIENTE";
                }

                session.setAttribute("loggedUser", user);
                session.setAttribute("rolElegido", interfaz);

                return switch (interfaz) {
                    case "ADMIN" -> "redirect:/admin/dashboard";
                    case "ABOGADO" -> "redirect:/abogado/dashboard";
                    default -> "redirect:/cliente/dashboard";
                };
            }

            // Si retorna false sin lanzar excepción, son credenciales incorrectas estándar
            model.addAttribute("error", "Credenciales incorrectas o usuario no encontrado");
            return "login";

        } catch (RuntimeException e) {
            // 2. MODIFICACIÓN DE SEGURIDAD: Captura los mensajes de RuntimeException enviados por AuthService (como el bloqueo de cuenta)
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new User());
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String register(@ModelAttribute("usuario") User user, Model model) {
        try {
            authService.register(user);
            return "redirect:/login?success";
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
            model.addAttribute("usuario", user);
            return "registro";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}