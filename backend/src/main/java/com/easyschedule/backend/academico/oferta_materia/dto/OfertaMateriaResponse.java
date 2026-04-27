package com.easyschedule.backend.academico.oferta_materia.dto;

public record OfertaMateriaResponse(
    Long id,
    String semestre,
    String paralelo,
    String docente,
    String aula
) {}
