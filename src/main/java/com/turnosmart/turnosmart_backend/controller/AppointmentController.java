package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.dto.AppointmentRequestDTO;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.LawyerService;
import com.turnosmart.turnosmart_backend.repository.ProcedureTypeRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        if (!"CLIENTE".equals(rol)) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("abogados", lawyerService.findAll());
        model.addAttribute("procedimientos", procedureTypeRepo.findAll());
        model.addAttribute("appointmentRequest", new AppointmentRequestDTO());
        return "cliente/nuevo-tramite";
    }

    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute AppointmentRequestDTO dto,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes, // Agregado para pasar mensajes flash tras la redirección
                                  Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        try {
            if ("JURIDICA".equals(dto.getRepresentationType()) && dto.getIdentifier() != null && !dto.getIdentifier().trim().isEmpty()) {
                String ruc = dto.getIdentifier().trim();
                if (!ruc.matches("^(10|20)\\d{9}$")) {
                    throw new com.turnosmart.turnosmart_backend.exception.BusinessException(
                            "Excepción E1: El identificador de la persona jurídica (RUC) ingresado no cumple con el formato válido de 11 dígitos."
                    );
                }
            }

            // 1. Crear el trámite (se asigna el abogado automáticamente por carga de trabajo)
            com.turnosmart.turnosmart_backend.dto.AppointmentResponseDTO nuevoTramite =
                    appointmentService.createAppointment(dto, loggedUser.getId());

            // 2. Recuperar la entidad completa mediante el ticket para extraer el abogado asignado
            Appointment expediente = appointmentService.findByTicket(nuevoTramite.getTicketNumber());
            User datosAbogado = expediente.getLawyer().getUser();
            String nombreCompletoAbogado = datosAbogado.getFirstName() + " " + datosAbogado.getLastName();

            // 3. Crear el mensaje de éxito dinámico usando Flash Attributes
            redirectAttributes.addFlashAttribute("successMessage",
                    "¡Trámite generado con éxito! Ticket: " + nuevoTramite.getTicketNumber() +
                            ". Asignado automáticamente al Especialista: " + nombreCompletoAbogado);

            return "redirect:/cliente/dashboard";

        } catch (com.turnosmart.turnosmart_backend.exception.BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("procedimientos", procedureTypeRepo.findAll());
            model.addAttribute("appointmentRequest", dto);
            return "cliente/nuevo-tramite";
        }
    }

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