package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MallaService {

	private final MallaRepository mallaRepository;
	private final MallaMateriaRepository mallaMateriaRepository;

	public MallaService(MallaRepository mallaRepository, MallaMateriaRepository mallaMateriaRepository) {
		this.mallaRepository = mallaRepository;
		this.mallaMateriaRepository = mallaMateriaRepository;
	}

	public List<MallaResponse> findActiveByCarrera(Long carreraId) {
		return mallaRepository.findByCarreraIdAndActiveTrueOrderByVersionDesc(carreraId).stream()
			.map((malla) -> new MallaResponse(
				malla.getId(),
				malla.getCarreraId(),
				malla.getNombre(),
				malla.getVersion(),
				malla.isActive()
			))
			.toList();
	}

	public List<MallaMateriaResponse> findMateriasByMalla(Long mallaId) {
		return mallaMateriaRepository.findByMallaIdAndMateriaActiveTrueOrderBySemestreSugeridoAsc(mallaId).stream()
			.map(mm -> new MallaMateriaResponse(
				mm.getId(),
				mm.getMateria().getId(),
				mm.getMateria().getCodigo(),
				mm.getMateria().getNombre(),
				mm.getSemestreSugerido()
			))
			.toList();
	}
}
