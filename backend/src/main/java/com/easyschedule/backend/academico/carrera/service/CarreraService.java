package com.easyschedule.backend.academico.carrera.service;

import com.easyschedule.backend.academico.carrera.dto.CarreraResponse;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CarreraService {

	private final CarreraRepository carreraRepository;

	public CarreraService(CarreraRepository carreraRepository) {
		this.carreraRepository = carreraRepository;
	}

	public List<CarreraResponse> findActiveByUniversidad(Long universidadId) {
		return carreraRepository.findByUniversidadIdAndActiveTrueOrderByNombreAsc(universidadId).stream()
			.map((carrera) -> new CarreraResponse(
				carrera.getId(),
				carrera.getUniversidadId(),
				carrera.getNombre(),
				carrera.getCodigo()
			))
			.toList();
	}
}
