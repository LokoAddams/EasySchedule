package com.easyschedule.backend.academico.malla.dto;

import java.util.List;

public record MallaEditResponse(
    Long mallaId,
    String nombre,
    String version,
    Long carreraId,
    List<MateriaImportRequest> materias
) {
}
