package com.turnosmart.turnosmart_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Activamos el interceptor protegiendo únicamente las rutas privadas
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/admin/**", "/abogado/**", "/cliente/**");
    }
}