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

    // =========================================================
    // DASHBOARDS
    // =========================================================

    @GetMapping("/abogado/dashboard")
    public String dashboardAbogado(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        List<Appointment> citas = appointmentRepository.findByLawyerId(user.getId());
        model.addAttribute("citasHoy", citas != null ? citas : new ArrayList<>());
        return "abogado/dashboard";
    }

    @GetMapping("/cliente/dashboard")
    public String dashboardCliente(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        List<Appointment> misTramites = appointmentRepository.findByClientId(user.getId());
        model.addAttribute("misTramites", misTramites != null ? misTramites : new ArrayList<>());
        return "cliente/dashboard";
    }

    // =========================================================
    // CATÁLOGO E INFORMACIÓN (Opción A)
    // =========================================================

    @GetMapping("/cliente/catalogo")
    public String verCatalogo(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        List<ProcedureType> lista = procedureTypeRepository.findAll();
        model.addAttribute("servicios", lista != null ? lista : new ArrayList<>());
        return "cliente/catalogo";
    }

    /**
     * Paso Intermedio: Muestra la información detallada del trámite.
     * Vinculado al botón "Solicitar información" del catálogo.
     */
    @GetMapping("/cliente/detalle/{id}")
    public String verDetalleTramite(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        ProcedureType tramite = procedureTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        model.addAttribute("tramite", tramite);
        return "cliente/detalle-tramite";
    }

    /**
     * Paso Final: Muestra el formulario de solicitud.
     * Vinculado al botón "Iniciar Trámite" dentro de la página de detalle.
     */
    @GetMapping("/cliente/iniciar-solicitud/{id}")
    public String mostrarFormularioSolicitud(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        ProcedureType tramite = procedureTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        AppointmentRequestDTO appointmentRequest = new AppointmentRequestDTO();
        appointmentRequest.setProcedureTypeId(tramite.getId());

        model.addAttribute("tramite", tramite);
        model.addAttribute("appointmentRequest", appointmentRequest);

        return "cliente/nuevo-tramite";
    }

    // =========================================================
    // GESTIÓN DE PERFIL
    // =========================================================

    @GetMapping("/cliente/perfil")
    public String verPerfil(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

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

    // =========================================================
    // ACCIONES DE TRÁMITE
    // =========================================================

    @PostMapping("/abogado/actualizar-estado")
    public String actualizarEstado(@RequestParam Long idCita,
                                   @RequestParam String nuevoEstado,
                                   @RequestParam(required = false) String comentario) {

        appointmentRepository.findById(idCita).ifPresent(appointment -> {
            appointment.setStatus(com.turnosmart.turnosmart_backend.entity.AppointmentStatus.valueOf(nuevoEstado));
            appointment.setNotes(comentario);
            appointmentRepository.save(appointment);
        });

        return "redirect:/abogado/dashboard?success";
    }
}