package com.easyschedule.backend.academico.estado_materia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EstadoMateriaRequest(
    @NotNull(message = "mallaMateriaId es requerido")
    Long mallaMateriaId,

    @NotNull(message = "estado es requerido")
    @Pattern(regexp = "^(aprobada|pendiente|cursando)$", message = "estado debe ser 'aprobada', 'pendiente' o 'cursando'")
    String estado
) {}
