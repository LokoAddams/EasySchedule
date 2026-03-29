package com.easyschedule.backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "El identificador es obligatorio")
    @Size(max = 50, message = "El identificador no puede exceder 50 caracteres")
    private String identifier; // username o email

    @NotBlank(message = "La contrasenia es obligatoria")
    @Size(max = 120, message = "La contrasenia no puede exceder 120 caracteres")
    private String password;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
