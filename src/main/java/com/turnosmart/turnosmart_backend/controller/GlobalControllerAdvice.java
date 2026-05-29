package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;

    @ModelAttribute
    public void addAttributes(Model model, Principal principal) {
        if (principal != null) {
            // Buscamos al usuario logueado por su email
            User user = userService.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
    }
}