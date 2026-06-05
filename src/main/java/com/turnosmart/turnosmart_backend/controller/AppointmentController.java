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
import org.springframework.web.multipart.MultipartFile;

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
                                  @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                  @RequestParam(value = "action", defaultValue = "enviar") String action,
                                  HttpSession session,
                                  Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        boolean esBorrador = "borrador".equals(action);

        try {
            if ("JURIDICA".equals(dto.getRepresentationType()) && dto.getIdentifier() != null && !dto.getIdentifier().trim().isEmpty()) {
                String ruc = dto.getIdentifier().trim();

                if (!ruc.matches("^(10|20)\\d{9}$")) {
                    throw new com.turnosmart.turnosmart_backend.exception.BusinessException(
                            "Excepción E1: El identificador de la persona jurídica (RUC) ingresado no cumple con el formato válido de 11 dígitos."
                    );
                }
            }

            if (!esBorrador && (files == null || files.isEmpty() || files.get(0).isEmpty())) {
                throw new com.turnosmart.turnosmart_backend.exception.BusinessException(
                        "RN-01: No se puede guardar ni enviar el trámite si falta cargar alguno de los documentos obligatorios."
                );
            }

            if (files != null && !files.isEmpty() && !files.get(0).isEmpty()) {
                for (MultipartFile file : files) {
                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                        throw new com.turnosmart.turnosmart_backend.exception.BusinessException(
                                "RN-02 / E1: Los documentos adjuntos deben subirse obligatoriamente en formato PDF."
                        );
                    }
                }
            }

            com.turnosmart.turnosmart_backend.dto.AppointmentResponseDTO nuevoTramite =
                    appointmentService.createAppointment(dto, loggedUser.getId(), esBorrador);

            if (files != null && !files.isEmpty() && !files.get(0).isEmpty()) {
                appointmentService.uploadDocuments(nuevoTramite.getId(), files, loggedUser.getId());
            }

            return "redirect:/cliente/dashboard?success";

        } catch (com.turnosmart.turnosmart_backend.exception.BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("abogados", lawyerService.findAll());
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