package com.easyschedule.backend.estudiante.dto;

public record AvanceGraduacionExport(
    byte[] contenido,
    String contentType,
    String filename
) {}
