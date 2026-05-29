package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Lawyer;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.AppointmentRepository;
import com.turnosmart.turnosmart_backend.service.AnalyticsService;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.LawyerService;
import com.turnosmart.turnosmart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    // Inyección de dependencias para manejar la persistencia y la lógica de negocio.
    private final AppointmentRepository appointmentRepository;
    private final AnalyticsService analyticsService;
    private final AppointmentService appointmentService;
    private final LawyerService lawyerService;
    private final UserService userService;

    // ==========================================
    // DASHBOARD ANALÍTICO
    // ==========================================

    // Carga la pantalla principal del administrador con estadísticas y el listado general de citas.
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(name = "filter", defaultValue = "mes") String filter, Model model) {
        // Recupera métricas según el rango de tiempo seleccionado en la interfaz (día, semana, mes, año).
        model.addAttribute("metrics", analyticsService.getDashboardMetrics(filter));
        model.addAttribute("currentFilter", filter);

        // Trae las citas cargando sus relaciones de golpe para que Thymeleaf no lance errores de sesión cerrada al renderizar.
        model.addAttribute("tramites", appointmentRepository.findAllWithDetails());

        return "admin/dashboard";
    }

    // ==========================================
    // GESTIÓN DE CLIENTES
    // ==========================================

    // Muestra la lista de usuarios que tienen el rol de clientes y están activos en la plataforma.
    @GetMapping("/usuarios")
    public String listarClientes(Model model, Principal principal) {
        // Si el administrador está logueado, pasamos sus datos a la vista para personalizar la interfaz.
        if (principal != null) {
            User currentUser = userService.findByEmail(principal.getName());
            model.addAttribute("user", currentUser);
        }

        // Filtramos la lista completa para quedarnos solo con los usuarios habilitados que sean clientes.
        List<User> usuariosActivos = userService.findAll().stream()
                .filter(u -> u.getEnabled() != null && u.getEnabled())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("CLIENTE")))
                .toList();

        model.addAttribute("usuarios", usuariosActivos);
        return "admin/usuarios";
    }

    // Muestra el perfil o expediente completo de un cliente específico basado en su ID.
    @GetMapping("/usuarios/detalle/{id}")
    public String verDetalleUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", userService.findById(id));
        return "admin/detalle-usuario";
    }

    // Realiza una baja lógica del cliente (desactivación) para no perder su historial de datos en la base de datos.
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        userService.deactivate(id);
        return "redirect:/admin/usuarios";
    }

    // ==========================================
    // GESTIÓN DE ABOGADOS (CRUD)
    // ==========================================

    // Lista a todos los abogados registrados en el sistema para su administración.
    @GetMapping("/abogados")
    public String listarAbogados(Model model) {
        model.addAttribute("abogados", lawyerService.findAll());
        return "admin/abogados";
    }

    // Prepara el formulario de registro para un nuevo abogado, inicializando el objeto de usuario vinculado.
    @GetMapping("/abogados/nuevo")
    public String formularioAbogado(Model model) {
        Lawyer lawyer = new Lawyer();
        lawyer.setUser(new User()); // Necesario para que los campos anidados (user.firstName, etc.) no rompan la vista.
        model.addAttribute("lawyer", lawyer);
        return "admin/nuevo-abogado";
    }

    // Busca los datos de un abogado existente y los carga en el mismo formulario para permitir su edición.
    @GetMapping("/abogados/editar/{id}")
    public String editarAbogado(@PathVariable Long id, Model model) {
        model.addAttribute("lawyer", lawyerService.findById(id));
        return "admin/nuevo-abogado";
    }

    // Procesa el formulario de los abogados, decidiendo si se trata de una actualización o un registro nuevo.
    @PostMapping("/abogados/guardar")
    public String guardarAbogado(@ModelAttribute Lawyer lawyer) {
        // Extrae el usuario asociado para manipular sus datos personales.
        User user = lawyer.getUser();

        if (lawyer.getId() != null) {
            // Flujo de actualización: Buscamos los datos reales de la base de datos para no pisar campos vacíos.
            Lawyer existingLawyer = lawyerService.findById(lawyer.getId());
            User existingUser = existingLawyer.getUser();

            // Sincronizamos los datos del usuario modificado.
            existingUser.setFirstName(user.getFirstName());
            existingUser.setLastName(user.getLastName());
            existingUser.setEmail(user.getEmail());
            existingUser.setDni(user.getDni());
            existingUser.setPhone(user.getPhone());

            // Sincronizamos los datos profesionales del abogado.
            existingLawyer.setColegiatura(lawyer.getColegiatura());
            existingLawyer.setSpecialization(lawyer.getSpecialization());
            existingLawyer.setBio(lawyer.getBio());

            lawyerService.save(existingLawyer);
        } else {
            // Flujo de creación: Activamos la cuenta por defecto y asignamos el DNI como contraseña provisional si viene vacía.
            user.setEnabled(true);
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(user.getDni());
            }
            lawyerService.save(lawyer);
        }

        return "redirect:/admin/abogados?success";
    }

    // Elimina físicamente o por cascada el registro de un abogado a través de su identificador.
    @GetMapping("/abogados/eliminar/{id}")
    public String eliminarAbogado(@PathVariable Long id) {
        lawyerService.delete(id);
        return "redirect:/admin/abogados?deleted";
    }
}