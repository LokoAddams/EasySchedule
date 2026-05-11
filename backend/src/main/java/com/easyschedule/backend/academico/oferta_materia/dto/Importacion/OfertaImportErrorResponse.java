package com.easyschedule.backend.academico.oferta_materia.dto.Importacion;

public record OfertaImportErrorResponse(
    int rowNumber,
    String field,
    String reason,
    boolean critical
) {
}
