package com.easyschedule.backend.academico.oferta_materia.dto.Importacion;

import java.util.List;

public record OfertaImportResultResponse(
    OfertaImportSummaryResponse summary,
    List<OfertaImportPreviewResponse> offers,
    List<OfertaImportErrorResponse> errors,
    List<OfertaImportWarningResponse> warnings
) {
}