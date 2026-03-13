package com.easyschedule.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record RegistroRequest(
    @NotBlank(message = "El nombre de usuario no puede estar vacio") String username,
    @NotBlank(message = "La contrasenia no puede estar vacia") String password,
    @NotBlank(message = "El correo no puede estar vacio") @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "El formato del correo electronico no es valido") String correo,
     String nombre,
     String apellido,
     String carnetIdentidad,
     LocalDate fechaNacimiento,
     Short semestreActual,
     String carrera,
     Long mallaId
) {}