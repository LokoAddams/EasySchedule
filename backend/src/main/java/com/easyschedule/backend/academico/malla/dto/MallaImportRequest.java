package com.easyschedule.backend.academico.malla.dto;

import java.util.List;

public record MallaImportRequest(
    String nombre,
    String version,
    Long carreraId,
    List<MateriaImportRequest> materias
) {
}
