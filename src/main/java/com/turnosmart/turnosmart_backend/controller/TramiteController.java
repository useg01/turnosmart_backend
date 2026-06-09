package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.entity.ProcedureType;
import com.turnosmart.turnosmart_backend.repository.AppointmentRepository;
import com.turnosmart.turnosmart_backend.repository.UserRepository;
import com.turnosmart.turnosmart_backend.repository.ProcedureTypeRepository;
import com.turnosmart.turnosmart_backend.dto.AppointmentRequestDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TramiteController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ProcedureTypeRepository procedureTypeRepository;

    @ModelAttribute
    public void addAttributes(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        String rol = (String) session.getAttribute("rolElegido");
        model.addAttribute("user", user != null ? user : new User());
        model.addAttribute("rol", rol != null ? rol : "INVITADO");
    }

    @GetMapping("/home")
    public String redirectByRol(HttpSession session) {
        String rol = (String) session.getAttribute("rolElegido");
        if ("ADMIN".equals(rol)) return "redirect:/admin/dashboard";
        if ("ABOGADO".equals(rol)) return "redirect:/abogado/dashboard";
        if ("CLIENTE".equals(rol)) return "redirect:/cliente/dashboard";
        return "redirect:/login";
    }

    @GetMapping("/abogado/dashboard")
    public String dashboardAbogado(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        List<Appointment> tramites = appointmentRepository.findAll();
        List<Appointment> filtrados = new ArrayList<>();

        if (tramites != null && loggedUser != null) {
            for (Appointment a : tramites) {
                if (a.getLawyer() != null && a.getLawyer().getUser() != null && loggedUser.getId().equals(a.getLawyer().getUser().getId())) {
                    filtrados.add(a);
                }
            }
        }

        model.addAttribute("tramites", filtrados);
        model.addAttribute("citas", filtrados);
        model.addAttribute("citasHoy", filtrados);
        model.addAttribute("total", filtrados.size());

        return "abogado/dashboard";
    }

    @GetMapping("/cliente/dashboard")
    public String clienteDashboard(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        List<Appointment> misTramites = appointmentRepository.findByClientId(loggedUser.getId());

        model.addAttribute("usuario", loggedUser);
        model.addAttribute("misTramites", misTramites != null ? misTramites : new ArrayList<>());

        return "cliente/dashboard";
    }

    @GetMapping("/cliente/catalogo")
    public String verCatalogo(Model model) {
        List<ProcedureType> lista = procedureTypeRepository.findAll();
        model.addAttribute("servicios", lista != null ? lista : new ArrayList<>());
        return "cliente/catalogo";
    }

    @GetMapping("/cliente/detalle/{id}")
    public String verDetalleTramite(@PathVariable Long id, Model model) {
        ProcedureType tramite = procedureTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        model.addAttribute("tramite", tramite);
        return "cliente/detalle-tramite";
    }

    @GetMapping("/cliente/iniciar-solicitud/{id}")
    public String mostrarFormularioSolicitud(@PathVariable Long id, Model model) {
        ProcedureType tramite = procedureTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        AppointmentRequestDTO appointmentRequest = new AppointmentRequestDTO();
        appointmentRequest.setProcedureTypeId(tramite.getId());

        model.addAttribute("tramite", tramite);
        model.addAttribute("appointmentRequest", appointmentRequest);

        return "cliente/nuevo-tramite";
    }

    @GetMapping("/cliente/perfil")
    public String verPerfil(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        model.addAttribute("usuario", user);
        return "cliente/perfil";
    }

    @PostMapping("/cliente/perfil/guardar")
    public String guardarPerfil(@ModelAttribute("usuario") User userForm, HttpSession session) {
        User userDb = userRepository.findById(userForm.getId()).orElse(null);

        if (userDb != null) {
            userDb.setFirstName(userForm.getFirstName());
            userDb.setLastName(userForm.getLastName());
            userDb.setEmail(userForm.getEmail());
            userDb.setPhone(userForm.getPhone());

            userRepository.save(userDb);
            session.setAttribute("loggedUser", userDb);
        }
        return "redirect:/cliente/dashboard?profileUpdated=true";
    }

    @PostMapping("/abogado/actualizar-estado")
    public String actualizarEstado(@RequestParam("idCita") Long idCita,
                                   @RequestParam("nuevoEstado") String nuevoEstado,
                                   @RequestParam(value = "comentario", required = false) String comentario) {

        Appointment appointment = appointmentRepository.findById(idCita)
                .orElseThrow(() -> new RuntimeException("No se encontró la cita con ID: " + idCita));

        try {
            com.turnosmart.turnosmart_backend.entity.AppointmentStatus statusEnum =
                    com.turnosmart.turnosmart_backend.entity.AppointmentStatus.valueOf(nuevoEstado.trim());
            appointment.setStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El estado enviado '" + nuevoEstado + "' no es un estado válido.");
        }

        appointment.setLawyerNotes(comentario);

        appointmentRepository.save(appointment);
        return "redirect:/abogado/dashboard?success=true";
    }
}