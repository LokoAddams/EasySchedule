package com.easyschedule.backend.academico.materia.dto;

public record PrerequisitoRequest(
    Long mallaMateriaId,
    Long prerequisitoMallaMateriaId
) {
}
