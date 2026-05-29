package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class NotarioController {

    private final LawyerService lawyerService;

    @GetMapping("/notarios")
    public String notarios(Model model) {

        model.addAttribute("lawyers", lawyerService.findAll());
        return "notarios";
    }
}