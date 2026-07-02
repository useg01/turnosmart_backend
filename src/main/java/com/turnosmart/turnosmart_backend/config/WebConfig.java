package com.turnosmart.turnosmart_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/admin/**", "/abogado/**", "/cliente/**");
    }

    /**
     * Expone la carpeta física donde FileService guarda los PDFs (DNI, recibos, etc.)
     * como recurso estático accesible vía /uploads/tramites/{archivo}.pdf
     * Esto permite que el abogado/admin pueda abrir o descargar los documentos
     * que el cliente sube desde el dashboard.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/tramites/**")
                .addResourceLocations("file:uploads/tramites/");

        registry.addResourceHandler("/uploads/cartas/**")
                .addResourceLocations("file:uploads/cartas/");
    }
}