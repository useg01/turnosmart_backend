package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;

import java.util.List;

@Controller
@RequestMapping("/abogado")
@RequiredArgsConstructor
public class LawyerController {

    private final AppointmentService appointmentService;

    @GetMapping("/bandeja")
    public String bandejaEntrada(@RequestParam Long lawyerId, Model model) {
        // Obtiene la lista de citas (Appointments) usando el service avanzado
        List<Appointment> tramites = appointmentService.findByLawyer(lawyerId);

        model.addAttribute("tramites", tramites);
        model.addAttribute("total", tramites.size());

        // Retorna la vista ubicada en templates/abogado/dashboard.html
        return "abogado/dashboard";
    }

    @PostMapping("/tramite/cambiar-estado")
    public String cambiarEstado(@RequestParam Long appId,
                                @RequestParam AppointmentStatus nuevoEstado,
                                @RequestParam String comentario,
                                HttpSession session) { // 1. Agregamos la sesión aquí

        // Obtenemos al abogado/admin logueado de la sesión
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        // Pasamos el cuarto argumento: loggedUser.getId()
        appointmentService.changeStatus(appId, nuevoEstado, comentario, loggedUser.getId());

        return "redirect:/admin/dashboard?actualizado";
    }
}
