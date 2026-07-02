package com.turnosmart.turnosmart_backend.config;

import com.turnosmart.turnosmart_backend.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        User loggedUser = (User) session.getAttribute("loggedUser");
        String rolElegido = (String) session.getAttribute("rolElegido");
        String requestURI = request.getRequestURI();

        if (loggedUser == null || rolElegido == null) {
            response.sendRedirect("/login?error=no_session");
            return false; // Detiene la petición por completo
        }

        if (requestURI.startsWith("/admin") && !"ADMIN".equals(rolElegido)) {
            response.sendRedirect("/login?error=acceso_denegado");
            return false;
        }

        if (requestURI.startsWith("/abogado") && !"ABOGADO".equals(rolElegido)) {
            response.sendRedirect("/login?error=acceso_denegado");
            return false;
        }

        if (requestURI.startsWith("/cliente") && !"CLIENTE".equals(rolElegido)) {
            response.sendRedirect("/login?error=acceso_denegado");
            return false;
        }

        return true;
    }
}