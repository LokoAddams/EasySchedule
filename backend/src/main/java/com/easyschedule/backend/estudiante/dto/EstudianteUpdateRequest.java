
package com.easyschedule.backend.estudiante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EstudianteUpdateRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String nombre,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    String apellido,

    @NotBlank(message = "El carnet de identidad es obligatorio")
    @Size(max = 30, message = "El carnet de identidad no puede exceder 30 caracteres")
    String carnetIdentidad,

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    LocalDate fechaNacimiento,

    @NotNull(message = "El semestre actual es obligatorio")
    Short semestreActual,
    Long universidadId,
    Long carreraId,
    Long mallaId
) {
}
