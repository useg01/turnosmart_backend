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
        // 1. Obtenemos el usuario logueado (que es una cuenta de tipo abogado/notario)
        User loggedUser = (User) session.getAttribute("loggedUser");

        // 2. Buscamos las citas usando el ID del usuario directamente en la Query
        // Tu AppointmentRepository usa: WHERE a.lawyer.id = :lawyerId.
        // Como en el script de inserción manual 'lawyers.id' coincide con 'users.id', usamos loggedUser.getId().
        List<Appointment> tramites = appointmentRepository.findByLawyerId(loggedUser.getId());

        // 3. Multi-inyectamos al modelo para blindar contra cualquier nombre que use tu HTML (tramites, citas, citasHoy)
        List<Appointment> listaSegura = (tramites != null) ? tramites : new ArrayList<>();

        model.addAttribute("tramites", listaSegura);
        model.addAttribute("citas", listaSegura);
        model.addAttribute("citasHoy", listaSegura);
        model.addAttribute("total", listaSegura.size());

        return "abogado/dashboard";
    }

    @GetMapping("/cliente/dashboard")
    public String clienteDashboard(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        // Cambiamos la llamada para usar findByClientId
        List<Appointment> misTramites = appointmentRepository.findByClientId(loggedUser.getId());

        model.addAttribute("usuario", loggedUser);
        model.addAttribute("misTramites", misTramites != null ? misTramites : new ArrayList<>());

        return "cliente/dashboard";
    }

    // =========================================================
    // CATÁLOGO E INFORMACIÓN
    // =========================================================

    @GetMapping("/cliente/catalogo")
    public String verCatalogo(Model model) {
        List<ProcedureType> lista = procedureTypeRepository.findAll();
        model.addAttribute("servicios", lista != null ? lista : new ArrayList<>());
        return "cliente/catalogo";
    }

    /**
     * Paso Intermedio: Muestra la información detallada del trámite.
     * Vinculado al botón "Solicitar información" del catálogo.
     */
    @GetMapping("/cliente/detalle/{id}")
    public String verDetalleTramite(@PathVariable Long id, Model model) {
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
    public String mostrarFormularioSolicitud(@PathVariable Long id, Model model) {
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

    // =========================================================
    // ACCIONES DE TRÁMITE (CORREGIDO)
    // =========================================================

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

        // CORRECCIÓN: Guardamos la respuesta en el campo del abogado sin tocar el del cliente
        appointment.setLawyerNotes(comentario);

        appointmentRepository.save(appointment);
        return "redirect:/abogado/dashboard?success=true";
    }
}