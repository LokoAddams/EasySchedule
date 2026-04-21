package com.easyschedule.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> originPatterns = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter((origin) -> !origin.isEmpty())
            .toList();

        registry.addMapping("/**")
            .allowedOriginPatterns(originPatterns.toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false);
    }
}
