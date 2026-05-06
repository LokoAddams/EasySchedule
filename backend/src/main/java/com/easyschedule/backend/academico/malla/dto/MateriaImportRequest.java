package com.easyschedule.backend.academico.malla.dto;

import java.util.List;

public record MateriaImportRequest(
    String codigo,
    String nombre,
    Integer semestre,
    Integer creditos,
    List<String> prerequisitos
) {
}
