package com.easyschedule.backend.academico.horario.dto;

import java.time.LocalTime;

public record ClaseBloqueDTO(
    String dia,
    LocalTime horaInicio,
    LocalTime horaFin
) {}
