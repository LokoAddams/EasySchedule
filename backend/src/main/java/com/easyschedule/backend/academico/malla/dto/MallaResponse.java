package com.easyschedule.backend.academico.malla.dto;

public record MallaResponse(
    Long id,
    Long carreraId,
    String nombre,
    String version,
    boolean active
) {
}
