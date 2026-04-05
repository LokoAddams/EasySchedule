package com.easyschedule.backend.academico.materia.dto;

public record MateriaRequest(
    String codigo,
    String nombre,
    Short creditos,
    Boolean active
) {
}
