package com.easyschedule.backend.academico.carrera.service;

import com.easyschedule.backend.academico.carrera.dto.CarreraRequest;
import com.easyschedule.backend.academico.carrera.dto.CarreraResponse;
import com.easyschedule.backend.academico.carrera.model.Carrera;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import com.easyschedule.backend.academico.universidad.repository.UniversidadRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CarreraService {

	private final CarreraRepository carreraRepository;
	private final UniversidadRepository universidadRepository;

	public CarreraService(CarreraRepository carreraRepository, UniversidadRepository universidadRepository) {
		this.carreraRepository = carreraRepository;
		this.universidadRepository = universidadRepository;
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

	public CarreraResponse createCarrera(CarreraRequest request) {
		if (request.nombre() == null || request.nombre().isBlank()) {
			throw new IllegalArgumentException("El nombre de la carrera es requerido");
		}
		if (request.codigo() == null || request.codigo().isBlank()) {
			throw new IllegalArgumentException("El codigo de la carrera es requerido");
		}
		if (request.universidadId() == null) {
			throw new IllegalArgumentException("El ID de la universidad es requerido");
		}

		if (!universidadRepository.existsById(request.universidadId())) {
			throw new IllegalArgumentException("La universidad con ID " + request.universidadId() + " no existe");
		}

		Carrera carrera = new Carrera();
		carrera.setNombre(request.nombre().trim());
		carrera.setCodigo(request.codigo().trim().toUpperCase());
		carrera.setUniversidadId(request.universidadId());
		carrera.setActive(request.active() != null ? request.active() : true);
		carrera = carreraRepository.save(carrera);

		return new CarreraResponse(
			carrera.getId(),
			carrera.getUniversidadId(),
			carrera.getNombre(),
			carrera.getCodigo()
		);
	}
}
