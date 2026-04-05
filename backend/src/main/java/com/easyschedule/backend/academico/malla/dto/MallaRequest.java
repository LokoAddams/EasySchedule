package com.easyschedule.backend.academico.malla.dto;

public record MallaRequest(
    Long carreraId,
    String nombre,
    String version,
    Boolean active
) {
}
