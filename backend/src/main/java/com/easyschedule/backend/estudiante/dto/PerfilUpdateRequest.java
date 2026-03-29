package com.easyschedule.backend.estudiante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PerfilUpdateRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 20, message = "El nombre de usuario no puede exceder 20 caracteres")
    String username,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String nombre,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    String apellido,

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "Formato de correo electronico invalido")
    @Size(max = 50, message = "El correo electronico no puede exceder 50 caracteres")
    String email,

    @NotBlank(message = "El carnet de identidad es obligatorio")
    @Size(max = 30, message = "El carnet de identidad no puede exceder 30 caracteres")
    String carnetIdentidad,

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    LocalDate fechaNacimiento,

    @Size(max = 120, message = "La carrera no puede exceder 120 caracteres")
    String carrera,

    @Size(max = 150, message = "La universidad no puede exceder 150 caracteres")
    String universidad
) {
}
