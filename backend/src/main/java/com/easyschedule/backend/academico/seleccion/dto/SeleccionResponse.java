package com.easyschedule.backend.academico.seleccion.dto;

public record SeleccionResponse(
	Long universidadId,
	String universidad,
	Long carreraId,
	String carrera,
	Long mallaId,
	String malla
) {
}
