package com.easyschedule.backend.academico.oferta_materia.dto.Importacion;

public record OfertaImportHorarioResponse(
    int rowNumber,
    String dia,
    String horaInicio,
    String horaFin
) {
}