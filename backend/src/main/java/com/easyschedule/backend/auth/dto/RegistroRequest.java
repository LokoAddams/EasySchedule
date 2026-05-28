package com.easyschedule.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.easyschedule.backend.shared.validation.PasswordPolicy;

public record RegistroRequest(
    @NotBlank(message = "El nombre de usuario no puede estar vacio") String username,
    @NotBlank(message = "La contrasenia no puede estar vacia")
    @Size(min = 8, max = 120, message = "La contrasenia debe tener entre 8 y 120 caracteres")
    @PasswordPolicy
    String password,
    @NotBlank(message = "El correo no puede estar vacio") @Email(message = "El formato del correo electronico no es valido") String email
) {}
