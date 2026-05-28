package com.easyschedule.backend.academico.carrera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CarreraRequest(
    @NotNull(message = "El ID de la universidad es requerido")
    Long universidadId,

    @NotBlank(message = "El nombre de la carrera es requerido")
    @Size(max = 150, message = "El nombre no debe exceder 150 caracteres")
    String nombre,

    @NotBlank(message = "El codigo de la carrera es requerido")
    @Size(max = 30, message = "El codigo no debe exceder 30 caracteres")
    String codigo,

    Boolean active
) {
}
