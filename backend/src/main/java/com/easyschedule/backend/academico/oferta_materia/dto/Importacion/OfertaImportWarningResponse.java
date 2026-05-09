package com.easyschedule.backend.academico.oferta_materia.dto.Importacion;

public record OfertaImportWarningResponse(
    int rowNumber,
    String field,
    String reason
) {
}