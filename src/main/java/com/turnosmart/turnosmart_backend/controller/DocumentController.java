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
    public String handleFileUpload(@PathVariable Long id,
                                   @RequestParam("fileDni") MultipartFile fileDni,
                                   @RequestParam("fileRecibo") MultipartFile fileRecibo,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            appointmentService.uploadDocuments(id, fileDni, fileRecibo, loggedUser.getId());

            redirectAttributes.addFlashAttribute("successMessage", "Documentos (DNI y Recibo) cargados correctamente.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir documentos: " + e.getMessage());
        }

        return "redirect:/cliente/dashboard";
    }
}