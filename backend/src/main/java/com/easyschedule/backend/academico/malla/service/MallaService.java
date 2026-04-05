package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MallaService {

	private final MallaRepository mallaRepository;

	public MallaService(MallaRepository mallaRepository) {
		this.mallaRepository = mallaRepository;
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
}
