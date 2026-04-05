package com.easyschedule.backend.academico.carrera.dto;

public record CarreraResponse(
	Long id,
	Long universidadId,
	String nombre,
	String codigo
) {
}
