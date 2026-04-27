package com.easyschedule.backend.academico.toma_materia.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TomaMateriaRequest(
    @NotEmpty List<Long> ofertaIds
) {
}
