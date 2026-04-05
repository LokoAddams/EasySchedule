package com.easyschedule.backend.academico.materia.dto;

public record MateriaResponse(
    Long id,
    String codigo,
    String nombre,
    Short creditos,
    boolean active
) {
}
