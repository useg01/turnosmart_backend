package com.turnosmart.turnosmart_backend.controller;

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
                              @RequestParam String interfaz,
                              HttpSession session,
                              Model model) {

        User user = userService.findByEmail(email);

        // Validación de credenciales
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("loggedUser", user);
            session.setAttribute("rolElegido", interfaz.toUpperCase());

            return switch (interfaz.toUpperCase()) {
                case "ADMIN" -> "redirect:/admin/dashboard";
                case "ABOGADO" -> "redirect:/abogado/dashboard";
                case "CLIENTE" -> "redirect:/cliente/dashboard";
                default -> "redirect:/login?error=rol_no_valido";
            };
        }

        model.addAttribute("error", "Credenciales incorrectas o usuario no encontrado");
        return "login";
    }

    // --- PANTALLA DE REGISTRO (GET) ---
    @GetMapping("/registro")
    public String registro(Model model) {
        // Usamos "usuario" como nombre de atributo para ser claros en el HTML
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