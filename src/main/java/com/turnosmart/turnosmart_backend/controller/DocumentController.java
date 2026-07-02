package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
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

    @PostMapping("/upload/{id}")
    public String handleFileUpload(@PathVariable Long id,
                                   @RequestParam("files") MultipartFile[] files,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        if (files == null || files.length == 0 || files[0].isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor, seleccione al menos un archivo.");
            return "redirect:/cliente/dashboard";
        }

        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            appointmentService.uploadDocuments(id, Arrays.asList(files), loggedUser.getId());

            redirectAttributes.addFlashAttribute("success", "Documentos cargados correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir documentos: " + e.getMessage());
        }

        return "redirect:/cliente/dashboard";
    }
}