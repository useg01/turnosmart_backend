package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.User; // Importación necesaria
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import jakarta.servlet.http.HttpSession; // Importación necesaria
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Controller
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final AppointmentService appointmentService;

    //Procesa la subida de documentos para un trámite específico.

    @PostMapping("/upload/{id}")
    public String handleFileUpload(@PathVariable Long id,
                                   @RequestParam("files") MultipartFile[] files,
                                   HttpSession session, // Se añade la sesión para obtener el usuario
                                   RedirectAttributes redirectAttributes) {

        // 1. Validación preventiva: verificar si hay archivos seleccionados
        if (files == null || files.length == 0 || files[0].isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor, seleccione al menos un archivo.");
            return "redirect:/cliente/dashboard";
        }

        // 2. Obtener el usuario de la sesión para la auditoría (Log)
        User loggedUser = (User) session.getAttribute("loggedUser");

        // Seguridad: Si no hay usuario en sesión, redirigir al login
        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            // 3. Llamamos al service pasando los 3 argumentos requeridos:
            // id de la cita, lista de archivos e ID del usuario que opera.
            appointmentService.uploadDocuments(id, Arrays.asList(files), loggedUser.getId());

            // Mensaje de éxito para la interfaz
            redirectAttributes.addFlashAttribute("success", "Documentos cargados correctamente.");
        } catch (Exception e) {
            // Captura errores de lógica de negocio o de guardado
            redirectAttributes.addFlashAttribute("error", "Error al subir documentos: " + e.getMessage());
        }

        // Redirigir al panel del cliente
        return "redirect:/cliente/dashboard";
    }
}