package com.easyschedule.backend.academico.horario.dto;

public record HorarioClaseResponse(
    String materia,
    String paralelo,
    String dia,
    String horaInicio,
    String horaFin,
    String docente,
    String aula,
    Integer creditos
) {
}
