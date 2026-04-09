package com.easyschedule.backend.academico.seleccion.dto;

import jakarta.validation.constraints.NotNull;

public record SeleccionRequest(
    @NotNull(message = "universidadId es obligatorio")
    Long universidadId,

    Long carreraId,

    @NotNull(message = "mallaId es obligatorio")
    Long mallaId
) {
}
