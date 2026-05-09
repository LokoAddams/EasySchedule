package com.easyschedule.backend.academico.oferta_materia.dto.Importacion;

public record OfertaImportSummaryResponse(
    int totalRows,
    int offersCreated,
    int offersUpdated,
    int scheduleBlocks,
    int skippedRows,
    int errorsCount,
    int warningsCount
) {
}
