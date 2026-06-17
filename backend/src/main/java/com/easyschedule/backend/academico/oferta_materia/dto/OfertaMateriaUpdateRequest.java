package com.easyschedule.backend.academico.oferta_materia.dto;

import java.util.List;

public record OfertaMateriaUpdateRequest(
    String codigoMateria,
    String nombreMateria,
    String paralelo,
    String semestre,
    String docente,
    String aula,
    List<HorarioDto> horarios
) {}
