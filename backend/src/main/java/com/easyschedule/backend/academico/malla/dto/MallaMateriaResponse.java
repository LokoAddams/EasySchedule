package com.easyschedule.backend.academico.malla.dto;

import java.util.List;

public record MallaMateriaResponse(
	Long id,
	Long materiaId,
	String codigoMateria,
	String nombreMateria,
	Short semestreSugerido,
	String estado,
	List<Long> prerequisitosIds
) {}
