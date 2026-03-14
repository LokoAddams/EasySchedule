package com.easyschedule.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
    @NotBlank(message = "El nombre de usuario no puede estar vacio") String username,
    @NotBlank(message = "La contrasenia no puede estar vacia") String password,
    @NotBlank(message = "El correo no puede estar vacio") @Email(message = "El formato del correo electronico no es valido") String email
) {}