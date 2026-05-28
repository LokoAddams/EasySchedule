package com.easyschedule.backend.academico.universidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UniversidadRequest(
    @NotBlank(message = "El nombre de la universidad es requerido")
    @Size(max = 150, message = "El nombre no debe exceder 150 caracteres")
    String nombre,

    @NotBlank(message = "El codigo de la universidad es requerido")
    @Size(max = 30, message = "El codigo no debe exceder 30 caracteres")
    String codigo,

    Boolean active
) {
}
