package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.dto.AppointmentRequestDTO;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.LawyerService;
import com.turnosmart.turnosmart_backend.repository.ProcedureTypeRepository; // Asumiendo que existe
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final LawyerService lawyerService;
    private final ProcedureTypeRepository procedureTypeRepo;

    @GetMapping("/crear")
    public String viewCrearTurno(Model model, HttpSession session) {
        String rol = (String) session.getAttribute("rolElegido");

        // Bloqueo: Si no es CLIENTE, no entra
        if (!"CLIENTE".equals(rol)) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("abogados", lawyerService.findAll());
        model.addAttribute("procedimientos", procedureTypeRepo.findAll());
        model.addAttribute("appointmentRequest", new AppointmentRequestDTO());
        return "cliente/nuevo-tramite";
    }

    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute AppointmentRequestDTO dto, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) return "redirect:/login";

        // El trámite se asigna automáticamente al usuario en sesión
        appointmentService.createAppointment(dto, loggedUser.getId());

        return "redirect:/cliente/dashboard?success";
    }

    // --- API PARA CALENDARIO ---

    @GetMapping("/api/dias-llenos")
    @ResponseBody
    public ResponseEntity<List<LocalDate>> getDiasLlenos(@RequestParam Long lawyerId) {
        return ResponseEntity.ok(appointmentService.findDiasOcupados(lawyerId));
    }

    @GetMapping("/api/slots-disponibles")
    @ResponseBody
    public ResponseEntity<List<String>> getSlots(@RequestParam Long lawyerId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(fecha, lawyerId));
    }
}