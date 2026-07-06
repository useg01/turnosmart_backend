package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.dto.AppointmentRequestDTO;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.LawyerService;
import com.turnosmart.turnosmart_backend.repository.ProcedureTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TurnoController {

    private final AppointmentService appointmentService;
    private final LawyerService lawyerService;
    private final ProcedureTypeRepository procedureRepo;

    @GetMapping("/turnos")
    public String turnos(Model model) {
        model.addAttribute("procedures", procedureRepo.findAll());
        model.addAttribute("lawyers", lawyerService.findAll());
        return "admin/turnos";
    }

    @GetMapping("/turnos/nuevo")
    public String nuevoTurno(Model model) {
        model.addAttribute("procedures", procedureRepo.findAll());
        model.addAttribute("lawyers", lawyerService.findAll());
        model.addAttribute("appointment", new AppointmentRequestDTO());
        return "admin/crear-turno";
    }

    @PostMapping("/turnos")
    public String guardarTurno(@ModelAttribute AppointmentRequestDTO dto,
                               @RequestParam Long clientId) {
        appointmentService.createAppointment(dto, clientId);
        return "redirect:/turnos";
    }
}