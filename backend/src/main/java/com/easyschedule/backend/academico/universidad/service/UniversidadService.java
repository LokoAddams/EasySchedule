package com.easyschedule.backend.academico.universidad.service;

import com.easyschedule.backend.academico.universidad.dto.UniversidadRequest;
import com.easyschedule.backend.academico.universidad.dto.UniversidadResponse;
import com.easyschedule.backend.academico.universidad.model.Universidad;
import com.easyschedule.backend.academico.universidad.repository.UniversidadRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UniversidadService {

    private final UniversidadRepository universidadRepository;

    public UniversidadService(UniversidadRepository universidadRepository) {
        this.universidadRepository = universidadRepository;
    }

    public List<UniversidadResponse> findAllActive() {
        return universidadRepository.findByActiveTrueOrderByNombreAsc().stream()
            .map((universidad) -> new UniversidadResponse(
                universidad.getId(),
                universidad.getNombre(),
                universidad.getCodigo()
            ))
            .toList();
    }

    public UniversidadResponse createUniversidad(UniversidadRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la universidad es requerido");
        }
        if (request.codigo() == null || request.codigo().isBlank()) {
            throw new IllegalArgumentException("El codigo de la universidad es requerido");
        }

        Universidad universidad = new Universidad();
        universidad.setNombre(request.nombre().trim());
        universidad.setCodigo(request.codigo().trim().toUpperCase());
        universidad.setActive(request.active() != null ? request.active() : true);
        universidad = universidadRepository.save(universidad);

        return new UniversidadResponse(
            universidad.getId(),
            universidad.getNombre(),
            universidad.getCodigo()
        );
    }
}
