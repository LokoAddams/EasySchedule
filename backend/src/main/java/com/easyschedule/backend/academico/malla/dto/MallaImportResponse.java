package com.easyschedule.backend.academico.malla.dto;

public record MallaImportResponse(
    Long mallaId,
    String nombre,
    int materiasImportadas,
    int prerequisitosImportados,
    String mensaje
) {
}
