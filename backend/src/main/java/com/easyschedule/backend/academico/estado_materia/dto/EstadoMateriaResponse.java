package com.easyschedule.backend.academico.estado_materia.dto;

import com.easyschedule.backend.academico.estado_materia.model.EstadoMateria;

import java.time.OffsetDateTime;

public record EstadoMateriaResponse(
    Long id,
    Long mallaMateriaId,
    String estado,
    OffsetDateTime fechaActualizacion
) {
    public static EstadoMateriaResponse fromEntity(EstadoMateria entity) {
        return new EstadoMateriaResponse(
            entity.getId(),
            entity.getMallaMateriaId(),
            entity.getEstado(),
            entity.getFechaActualizacion()
        );
    }
}
