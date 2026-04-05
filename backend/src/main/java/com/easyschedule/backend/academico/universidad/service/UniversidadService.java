package com.easyschedule.backend.academico.universidad.service;

import com.easyschedule.backend.academico.universidad.dto.UniversidadResponse;
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
}
