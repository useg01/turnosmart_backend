package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.exception.BusinessException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final AppointmentService appointmentService;

    @PostMapping("/upload/{id}")
    public String handleFileUpload(
            @PathVariable Long id,
            @RequestParam("fileDniOtorgante")      MultipartFile fileDniOtorgante,
            @RequestParam("fileReciboOtorgante")   MultipartFile fileReciboOtorgante,
            @RequestParam("fileDniRepresentante")  MultipartFile fileDniRepresentante,
            @RequestParam("fileReciboRepresentante") MultipartFile fileReciboRepresentante,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        try {
            appointmentService.uploadDocuments(
                    id,
                    fileDniOtorgante,
                    fileReciboOtorgante,
                    fileDniRepresentante,
                    fileReciboRepresentante,
                    loggedUser.getId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Documentos cargados correctamente.");

        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al subir documentos: " + e.getMessage());
        }

        return "redirect:/cliente/dashboard";
    }
}