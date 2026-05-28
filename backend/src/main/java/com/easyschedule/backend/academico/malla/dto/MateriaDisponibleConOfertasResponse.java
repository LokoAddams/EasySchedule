package com.easyschedule.backend.academico.malla.dto;

import java.util.List;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaResponse;

public record MateriaDisponibleConOfertasResponse(
	Long id,
	Long materiaId,
	String codigoMateria,
	String nombreMateria,
	Short creditos,
	Short semestreSugerido,
	String estado,
	List<Long> prerequisitosIds,
	List<OfertaMateriaResponse> ofertas
) {}
