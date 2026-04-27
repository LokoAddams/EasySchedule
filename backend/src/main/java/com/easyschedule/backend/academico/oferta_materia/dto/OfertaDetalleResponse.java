package com.easyschedule.backend.academico.oferta_materia.dto;

import java.util.List;

public record OfertaDetalleResponse(
    Long mallaMateriaId,
    String nombreMateria,
    int creditos,
    List<String> prerequisitos,
    List<OfertaMateriaResponse> gruposDisponibles
) {}
