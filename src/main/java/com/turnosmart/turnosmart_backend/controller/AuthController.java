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

        User user = userService.findByEmail(email);

        if (user != null && user.getPassword().equals(password)) {

            // 1. Validar que el Set de roles no esté vacío
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                model.addAttribute("error", "El usuario no tiene un rol asignado en el sistema.");
                return "login";
            }

            // 2. Extraer todos los nombres de los roles que posee el usuario en una lista limpia
            List<String> roleNames = user.getRoles().stream()
                    .map(role -> role.getName().toUpperCase())
                    .toList();

            // 3. Determinar la interfaz por orden de jerarquía estricta
            String interfaz;

            if (roleNames.contains("ROLE_ADMIN") || roleNames.contains("ADMIN")) {
                interfaz = "ADMIN";
            } else if (roleNames.contains("ROLE_NOTARIO") || roleNames.contains("NOTARIO") || roleNames.contains("ROLE_ABOGADO") || roleNames.contains("ABOGADO")) {
                interfaz = "ABOGADO";
            } else {
                interfaz = "CLIENTE";
            }

            // 4. Guardar en sesión de forma segura
            session.setAttribute("loggedUser", user);
            session.setAttribute("rolElegido", interfaz);

            // 5. Redirección automática basada en la jerarquía detectada
            return switch (interfaz) {
                case "ADMIN" -> "redirect:/admin/dashboard";
                case "ABOGADO" -> "redirect:/abogado/dashboard";
                default -> "redirect:/cliente/dashboard";
            };
        }

        model.addAttribute("error", "Credenciales incorrectas o usuario no encontrado");
        return "login";
    }

    // --- PANTALLA DE REGISTRO (GET) ---
    @GetMapping("/registro")
    public String registro(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new User());
        }
        return "registro";
    }

    // --- PROCESAR EL REGISTRO (POST) ---
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
        return "redirect:/login?logout";
    }
}