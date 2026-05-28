package com.easyschedule.backend.academico.horario.dto;

import java.util.List;

public record HorarioGeneradorRequest(
    Long userId,
    Long mallaId,
    List<MateriaSeleccionadaRequest> materiasSeleccionadas,
    List<String> prioridades
) {}
