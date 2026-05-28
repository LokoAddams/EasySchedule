package com.easyschedule.backend.academico.materia.dto;

public record PrerequisitoResponse(
    Long id,
    Long mallaMateriaId,
    Long prerequisitoMallaMateriaId,
    String codigoMateria,
    String nombreMateria,
    String codigoPrerequisito,
    String nombrePrerequisito,
    Short semestreSugeridoMateria,
    Short semestreSugeridoPrerequisito
) {
}
