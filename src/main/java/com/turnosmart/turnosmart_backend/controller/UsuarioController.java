package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UserService userService;
    private final AppointmentService appointmentService;

    // VER PERFIL (Modo Lectura)
    @GetMapping("/perfil/{id}")
    public String verPerfil(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "perfil";
    }

    // CARGAR FORMULARIO DE EDICIÓN (Nuevo)
    @GetMapping("/perfil/editar/{id}")
    public String editarPerfil(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "perfil-editar"; // Esta será la vista con el DNI readonly
    }

    // PROCESAR ACTUALIZACIÓN
    @PostMapping("/perfil/update")
    public String actualizarPerfil(@ModelAttribute User user) {
        userService.update(user.getId(), user); // Este método ya protege el DNI
        return "redirect:/usuario/perfil/" + user.getId();
    }

    // PANEL "MIS TRÁMITES"
    @GetMapping("/mis-tramites")
    public String listarMisTramites(@RequestParam Long clientId, Model model) {
        List<Appointment> misTurnos = appointmentService.findByClient(clientId);
        model.addAttribute("tramites", misTurnos);
        return "mis-tramites";
    }

    @GetMapping("/seguimiento/{ticket}")
    public String detalleSeguimiento(@PathVariable String ticket, Model model) {
        Appointment app = appointmentService.findByTicket(ticket);
        model.addAttribute("app", app);
        boolean puedeCorregir = app.getStatus().name().equals("REGULARIZAR");
        model.addAttribute("puedeCorregir", puedeCorregir);
        return "detalle-tramite";
    }

    @GetMapping("/admin/lista")
    public String listaUsuariosAdmin(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/usuarios";
    }
}