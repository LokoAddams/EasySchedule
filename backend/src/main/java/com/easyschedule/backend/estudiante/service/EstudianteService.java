package com.easyschedule.backend.estudiante.service;

import com.easyschedule.backend.estudiante.dto.EstudianteResponse;
import com.easyschedule.backend.estudiante.dto.EstudianteUpdateRequest;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.easyschedule.backend.malla.model.Malla;
import com.easyschedule.backend.malla.repository.MallaRepository;
import com.easyschedule.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final MallaRepository mallaRepository;

    public EstudianteService(EstudianteRepository estudianteRepository, MallaRepository mallaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.mallaRepository = mallaRepository;
    }

    public List<EstudianteResponse> findAll() {
        return estudianteRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EstudianteResponse findById(Long id) {
        return toResponse(getEstudianteOrThrow(id));
    }

    public EstudianteResponse update(Long id, EstudianteUpdateRequest request) {
        Estudiante estudiante = getEstudianteOrThrow(id);
        Malla malla = getMallaOrThrow(request.mallaId());

        estudiante.setNombre(request.nombre());
        estudiante.setApellido(request.apellido());
        estudiante.setCarnetIdentidad(request.carnetIdentidad());
        estudiante.setFechaNacimiento(request.fechaNacimiento());
        estudiante.setSemestreActual(request.semestreActual());
        estudiante.setCarrera(request.carrera());
        estudiante.setMalla(malla);

        return toResponse(estudianteRepository.save(estudiante));
    }

    public void delete(Long id) {
        Estudiante estudiante = getEstudianteOrThrow(id);
        estudianteRepository.delete(estudiante);
    }

    private Estudiante getEstudianteOrThrow(Long id) {
        return estudianteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
    }

    private Malla getMallaOrThrow(Long id) {
        return mallaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Malla no encontrada con id: " + id));
    }

    private EstudianteResponse toResponse(Estudiante estudiante) {
        Long mallaId = estudiante.getMalla() != null ? estudiante.getMalla().getId() : null;
        String username = estudiante.getUser() != null ? estudiante.getUser().getUsername() : null;
        String email = estudiante.getUser() != null ? estudiante.getUser().getEmail() : null;

        return new EstudianteResponse(
            estudiante.getId(),
            username,
            estudiante.getNombre(),
            estudiante.getApellido(),
            email,
            estudiante.getCarnetIdentidad(),
            estudiante.getFechaNacimiento(),
            estudiante.getFechaRegistro(),
            estudiante.getSemestreActual(),
            estudiante.getCarrera(),
            mallaId
        );
    }
}