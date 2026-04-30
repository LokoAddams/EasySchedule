package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import com.easyschedule.backend.academico.estado_materia.service.EstadoMateriaService;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MallaService {

	private final MallaRepository mallaRepository;
	private final MallaMateriaRepository mallaMateriaRepository;
	private final EstadoMateriaService estadoMateriaService;
	private final PrerequisitoRepository prerequisitoRepository;

	public MallaService(MallaRepository mallaRepository, MallaMateriaRepository mallaMateriaRepository, EstadoMateriaService estadoMateriaService, PrerequisitoRepository prerequisitoRepository) {
		this.mallaRepository = mallaRepository;
		this.mallaMateriaRepository = mallaMateriaRepository;
		this.estadoMateriaService = estadoMateriaService;
		this.prerequisitoRepository = prerequisitoRepository;
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

	public List<MallaMateriaResponse> findMateriasByMalla(Long mallaId, Long userId) {
		return mallaMateriaRepository.findByMallaIdAndMateriaActiveTrueOrderBySemestreSugeridoAsc(mallaId).stream()
			.map(mm -> {
				String estado = estadoMateriaService.getEstadoMateria(userId, mm.getId());
				List<Long> prereqs = prerequisitoRepository.findByMallaMateria_Id(mm.getId()).stream()
					.map(p -> p.getPrerequisito().getId())
					.toList();
				return new MallaMateriaResponse(
					mm.getId(),
					mm.getMateria().getId(),
					mm.getMateria().getCodigo(),
					mm.getMateria().getNombre(),
					mm.getSemestreSugerido(),
					estado,
					prereqs
				);
			})
			.toList();
	}
}
