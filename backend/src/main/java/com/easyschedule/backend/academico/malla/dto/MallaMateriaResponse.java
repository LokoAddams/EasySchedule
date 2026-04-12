package com.easyschedule.backend.academico.malla.dto;

public record MallaMateriaResponse(
	Long id,
	Long materiaId,
	String codigoMateria,
	String nombreMateria,
	Short semestreSugerido
) {}
