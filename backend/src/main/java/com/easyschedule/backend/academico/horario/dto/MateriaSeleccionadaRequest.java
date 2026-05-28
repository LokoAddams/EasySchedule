package com.easyschedule.backend.academico.horario.dto;

import java.util.List;

public record MateriaSeleccionadaRequest(
    Long materiaId,
    List<String> paralelos
) {}
