package com.easyschedule.backend.academico.malla.dto;

public record MallaMateriaRequest(
    Long mallaId,
    Long materiaId,
    Short semestreSugerido
) {
}
