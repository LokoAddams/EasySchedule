package com.easyschedule.backend.academico.oferta_materia.dto;

public record OfertaMateriaListResponse(
    Long id,
    String codigoMateria,
    String nombreMateria,
    String semestre,
    String paralelo,
    String docente,
    String aula
) {}
