package com.easyschedule.backend.estudiante.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EstudianteResponse(
    Long id,
    String username,
    String nombre,
    String apellido,
    String email,
    String carnetIdentidad,
    LocalDate fechaNacimiento,
    OffsetDateTime fechaRegistro,
    Short semestreActual,
    String carrera,
    Long mallaId,
    String universidad
) {
}
