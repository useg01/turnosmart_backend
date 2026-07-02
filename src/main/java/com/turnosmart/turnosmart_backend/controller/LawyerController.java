package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.DocumentGeneratorService;
import com.turnosmart.turnosmart_backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
@RequestMapping("/abogado")
@RequiredArgsConstructor
public class LawyerController {

    private final AppointmentService appointmentService;
    private final DocumentGeneratorService documentGeneratorService;

    @GetMapping("/bandeja")
    public String bandejaEntrada(@RequestParam Long lawyerId, Model model) {
        List<Appointment> tramites = appointmentService.findByLawyer(lawyerId);
        model.addAttribute("tramites", tramites);
        model.addAttribute("total", tramites.size());
        return "abogado/dashboard";
    }

    @GetMapping("/tramite/evaluar/{id}")
    public String verEvaluacionExpediente(@PathVariable Long id, Model model, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        Appointment expediente = appointmentService.findAll().stream()
                .filter(x -> x.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No se encontró el expediente solicitado."));

        model.addAttribute("expediente", expediente);
        return "abogado/evaluar-expediente";
    }

    @PostMapping("/tramite/evaluar/{id}")
    public String evaluarExpediente(@PathVariable Long id,
                                    @RequestParam("accion") String accion,
                                    @RequestParam(value = "comentario", required = false) String comentario,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        try {
            AppointmentStatus nuevoEstado;
            String mensajeFeed;

            if ("continuar".equals(accion)) {
                nuevoEstado = AppointmentStatus.CONFORME;
                mensajeFeed = "¡Excelente! El expediente ha sido aprobado y continúa con el proceso.";
            } else {
                if (comentario == null || comentario.trim().isEmpty()) {
                    throw new BusinessException("Debe ingresar un comentario obligatoriamente si decide detener el proceso.");
                }
                nuevoEstado = AppointmentStatus.PROCESO_DETENIDO;
                mensajeFeed = "El proceso ha sido detenido y se registraron las observaciones para el cliente.";
            }

            appointmentService.changeStatus(id, nuevoEstado, comentario, loggedUser.getId());

            redirectAttributes.addFlashAttribute("success", mensajeFeed);

            return "redirect:/abogado/bandeja?lawyerId=" + loggedUser.getId();

        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/abogado/tramite/evaluar/" + id;
        }
    }

    /**
     * El abogado revisa los documentos (DNI y Recibo) que el cliente subió
     * mientras el trámite estaba en DOCUMENTOS_ENVIADOS, y decide:
     * - Aprobar: el trámite pasa a ENTREGADO (Trámite Completado).
     * - Rechazar: el trámite vuelve a REGULARIZAR para que el cliente
     *   corrija o vuelva a subir los documentos correctos.
     */
    @PostMapping("/tramite/documentos/{id}")
    public String evaluarDocumentos(@PathVariable Long id,
                                    @RequestParam("accion") String accion,
                                    @RequestParam(value = "comentario", required = false) String comentario,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        try {
            AppointmentStatus nuevoEstado;
            String mensajeFeed;

            if ("aprobar".equals(accion)) {
                nuevoEstado = AppointmentStatus.ENTREGADO;

                // Generar el documento legal (Carta de Poder / Rep. Legal)
                Appointment appParaGenerar = appointmentService.findAll().stream()
                        .filter(x -> x.getId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("Trámite no encontrado."));

                String rutaCarta = documentGeneratorService.generarCarta(appParaGenerar);
                appointmentService.guardarRutaCarta(id, rutaCarta, loggedUser.getId());

                mensajeFeed = "Documentos aprobados. La carta legal fue generada exitosamente y el trámite ha sido completado.";
            } else if ("rechazar".equals(accion)) {
                if (comentario == null || comentario.trim().isEmpty()) {
                    throw new BusinessException("Debe ingresar un comentario indicando por qué se rechazan los documentos.");
                }
                nuevoEstado = AppointmentStatus.REGULARIZAR;
                mensajeFeed = "Documentos rechazados. Se notificó al cliente para que los corrija.";
            } else {
                throw new BusinessException("Acción no reconocida.");
            }

            appointmentService.changeStatus(id, nuevoEstado, comentario, loggedUser.getId());
            redirectAttributes.addFlashAttribute("success", mensajeFeed);

        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/abogado/bandeja?lawyerId=" + loggedUser.getId();
    }

    @PostMapping("/tramite/cambiar-estado")
    public String cambiarEstado(@RequestParam Long appId,
                                @RequestParam AppointmentStatus nuevoEstado,
                                @RequestParam String comentario,
                                HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        appointmentService.changeStatus(appId, nuevoEstado, comentario, loggedUser.getId());
        return "redirect:/admin/dashboard?actualizado";
    }
}