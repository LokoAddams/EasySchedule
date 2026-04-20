package com.easyschedule.backend.academico.estado_materia.service;

import com.easyschedule.backend.academico.estado_materia.dto.EstadoMateriaRequest;
import com.easyschedule.backend.academico.estado_materia.dto.EstadoMateriaResponse;
import com.easyschedule.backend.academico.estado_materia.model.EstadoMateria;
import com.easyschedule.backend.academico.estado_materia.repository.EstadoMateriaRepository;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EstadoMateriaService {

    private static final Logger log = LoggerFactory.getLogger(EstadoMateriaService.class);

    private final EstadoMateriaRepository estadoMateriaRepository;
    private final MallaMateriaRepository mallaMateriaRepository;
    private final EstudianteRepository estudianteRepository;

    public EstadoMateriaService(
        EstadoMateriaRepository estadoMateriaRepository,
        MallaMateriaRepository mallaMateriaRepository,
        EstudianteRepository estudianteRepository
    ) {
        this.estadoMateriaRepository = estadoMateriaRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
        this.estudianteRepository = estudianteRepository;
    }

    public List<EstadoMateriaResponse> getEstadosByUserId(Long userId) {
        return estadoMateriaRepository.findByUserId(userId).stream()
            .map(EstadoMateriaResponse::fromEntity)
            .toList();
    }

    public List<EstadoMateriaResponse> getEstadosByMalla(Long userId, Long mallaId) {
        return estadoMateriaRepository.findByUserIdAndMallaId(userId, mallaId).stream()
            .map(EstadoMateriaResponse::fromEntity)
            .toList();
    }

    @Transactional
    public EstadoMateriaResponse saveEstado(Long userId, EstadoMateriaRequest request) {
        validarPropiedadMallaMateria(userId, request.mallaMateriaId());

        Optional<EstadoMateria> existenteOpt = estadoMateriaRepository.findByUserIdAndMallaMateriaId(
            userId, request.mallaMateriaId()
        );

        EstadoMateria estado;
        if (existenteOpt.isPresent()) {
            estado = existenteOpt.get();
            log.debug("[ESTADO_MATERIA_SAVE] actualizando estado existente | userId={} mallaMateriaId={}", userId, request.mallaMateriaId());
            estado.setEstado(request.estado());
            estado.setFechaActualizacion(OffsetDateTime.now());
        } else {
            log.debug("[ESTADO_MATERIA_SAVE] creando nuevo estado | userId={} mallaMateriaId={}", userId, request.mallaMateriaId());
            estado = new EstadoMateria(userId, request.mallaMateriaId(), request.estado());
        }

        EstadoMateria saved = estadoMateriaRepository.save(estado);
        log.info("[ESTADO_MATERIA_SAVE] estado guardado exitosamente | userId={} id={}", userId, saved.getId());
        return EstadoMateriaResponse.fromEntity(saved);
    }

    @Transactional
    public List<EstadoMateriaResponse> saveEstados(Long userId, List<EstadoMateriaRequest> requests) {
        log.info("[ESTADO_MATERIA_BATCH] procesando {} estados en lote | userId={}", requests.size(), userId);
        return requests.stream()
            .map(req -> saveEstadoInternal(userId, req))
            .toList();
    }

    private EstadoMateriaResponse saveEstadoInternal(Long userId, EstadoMateriaRequest request) {
        validarPropiedadMallaMateria(userId, request.mallaMateriaId());

        Optional<EstadoMateria> existenteOpt = estadoMateriaRepository.findByUserIdAndMallaMateriaId(
            userId, request.mallaMateriaId()
        );

        EstadoMateria estado;
        if (existenteOpt.isPresent()) {
            estado = existenteOpt.get();
            log.debug("[ESTADO_MATERIA_SAVE] actualizando estado existente | userId={} mallaMateriaId={}", userId, request.mallaMateriaId());
            estado.setEstado(request.estado());
            estado.setFechaActualizacion(OffsetDateTime.now());
        } else {
            log.debug("[ESTADO_MATERIA_SAVE] creando nuevo estado | userId={} mallaMateriaId={}", userId, request.mallaMateriaId());
            estado = new EstadoMateria(userId, request.mallaMateriaId(), request.estado());
        }

        EstadoMateria saved = estadoMateriaRepository.save(estado);
        log.info("[ESTADO_MATERIA_SAVE] estado guardado exitosamente | userId={} id={}", userId, saved.getId());
        return EstadoMateriaResponse.fromEntity(saved);
    }

    private void validarPropiedadMallaMateria(Long userId, Long mallaMateriaId) {
        Estudiante estudiante = estudianteRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no encontrado"));

        if (estudiante.getMalla() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sin malla asignada");
        }

        Optional<MallaMateria> mallaMateriaOpt = mallaMateriaRepository.findById(mallaMateriaId);
        if (mallaMateriaOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MallaMateria no encontrada");
        }

        MallaMateria mallaMateria = mallaMateriaOpt.get();
        if (!mallaMateria.getMalla().getId().equals(estudiante.getMalla().getId())) {
            log.warn("[IDOR_BLOCKED] intento de acceso no autorizado | userId={} mallaMateriaId={} mallaUsuarioId={}", 
                userId, mallaMateriaId, estudiante.getMalla().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MallaMateria no pertenece al curriculum del usuario");
        }
    }
}
