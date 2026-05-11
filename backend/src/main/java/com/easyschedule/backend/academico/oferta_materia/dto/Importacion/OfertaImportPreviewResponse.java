package com.easyschedule.backend.academico.oferta_materia.dto.Importacion;

import java.util.List;

public record OfertaImportPreviewResponse(
    String codigoMateria,
    String nombreMateria,
    Long mallaMateriaId,
    String paralelo,
    String semestreAcademico,
    String docente,
    String aula,
    List<OfertaImportHorarioResponse> horarios
) {
}