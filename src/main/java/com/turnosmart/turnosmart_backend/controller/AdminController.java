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

    private final AppointmentRepository appointmentRepository;
    private final AnalyticsService analyticsService;
    private final AppointmentService appointmentService;
    private final LawyerService lawyerService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(name = "filter", defaultValue = "mes") String filter, Model model) {
        model.addAttribute("metrics", analyticsService.getDashboardMetrics(filter));
        model.addAttribute("currentFilter", filter);


        model.addAttribute("tramites", appointmentRepository.findAllWithDetails());

        return "admin/dashboard";
    }

    @GetMapping("/usuarios")
    public String listarClientes(Model model, Principal principal) {
        if (principal != null) {
            User currentUser = userService.findByEmail(principal.getName());
            model.addAttribute("user", currentUser);
        }

        List<User> usuariosActivos = userService.findAll().stream()
                .filter(u -> u.getEnabled() != null && u.getEnabled())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("CLIENTE")))
                .toList();

        model.addAttribute("usuarios", usuariosActivos);
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/detalle/{id}")
    public String verDetalleUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", userService.findById(id));
        return "admin/detalle-usuario";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        userService.deactivate(id);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/abogados")
    public String listarAbogados(Model model) {
        model.addAttribute("abogados", lawyerService.findAll());
        return "admin/abogados";
    }

    @GetMapping("/abogados/nuevo")
    public String formularioAbogado(Model model) {
        Lawyer lawyer = new Lawyer();
        lawyer.setUser(new User());
        model.addAttribute("lawyer", lawyer);
        return "admin/nuevo-abogado";
    }

    @GetMapping("/abogados/editar/{id}")
    public String editarAbogado(@PathVariable Long id, Model model) {
        model.addAttribute("lawyer", lawyerService.findById(id));
        return "admin/nuevo-abogado";
    }

    @PostMapping("/abogados/guardar")
    public String guardarAbogado(@ModelAttribute Lawyer lawyer, Model model) {
        User user = lawyer.getUser();

        try {
            if (lawyer.getId() != null) {
                Lawyer existingLawyer = lawyerService.findById(lawyer.getId());
                User existingUser = existingLawyer.getUser();

                existingUser.setFirstName(user.getFirstName());
                existingUser.setLastName(user.getLastName());
                existingUser.setEmail(user.getEmail());
                existingUser.setDni(user.getDni());
                existingUser.setPhone(user.getPhone());

                existingLawyer.setColegiatura(lawyer.getColegiatura());
                existingLawyer.setSpecialization(lawyer.getSpecialization());
                existingLawyer.setBio(lawyer.getBio());

                lawyerService.save(existingLawyer);
            } else {
                user.setEnabled(true);
                if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    user.setPassword(user.getDni());
                }
                lawyerService.save(lawyer);
            }

            return "redirect:/admin/abogados?success";

        } catch (com.turnosmart.turnosmart_backend.exception.BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("lawyer", lawyer);
            return "admin/nuevo-abogado";
        }
    }

    @GetMapping("/abogados/eliminar/{id}")
    public String eliminarAbogado(@PathVariable Long id) {
        lawyerService.delete(id);
        return "redirect:/admin/abogados?deleted";
    }
}