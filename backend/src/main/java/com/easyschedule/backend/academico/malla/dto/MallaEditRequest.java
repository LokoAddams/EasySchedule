package com.easyschedule.backend.academico.malla.dto;

import java.util.List;

public record MallaEditRequest(
    List<MateriaImportRequest> materias
) {
}
