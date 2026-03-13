package com.easyschedule.backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF para APIs stateless
            .csrf(csrf -> csrf.disable())
            // Configurar la política de sesión como STATELESS
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Definir reglas de autorización
            .authorizeHttpRequests(auth -> auth
                // Permitir acceso público al endpoint de registro
                .requestMatchers(HttpMethod.POST, "/api/registro").permitAll()
                // Permitir acceso público a los endpoints de estudiantes (para pruebas)
                .requestMatchers("/api/estudiantes/**").permitAll()
                // Permitir acceso público a los endpoints de mallas (para pruebas)
                .requestMatchers("/api/mallas/**").permitAll()
                // Requerir autenticación para cualquier otra petición
                .anyRequest().authenticated()
            );

        return http.build();
    }
}