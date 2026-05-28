package com.easyschedule.backend.academico.materia.service;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.dto.PrerequisitoRequest;
import com.easyschedule.backend.academico.materia.dto.PrerequisitoResponse;
import com.easyschedule.backend.academico.materia.model.Prerequisito;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MateriaService {

    private static final Logger log = LoggerFactory.getLogger(MateriaService.class);

    private final PrerequisitoRepository prerequisitoRepository;
    private final MallaMateriaRepository mallaMateriaRepository;

    public MateriaService(PrerequisitoRepository prerequisitoRepository,
                          MallaMateriaRepository mallaMateriaRepository) {
        this.prerequisitoRepository = prerequisitoRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
    }

    @Transactional
    public PrerequisitoResponse crearPrerequisito(PrerequisitoRequest request) {
        Long materiaId = request.mallaMateriaId();
        Long prereqId = request.prerequisitoMallaMateriaId();

        log.info("Creando prerrequisito | mallaMateriaId={} prerequisitoMallaMateriaId={}", materiaId, prereqId);

        if (materiaId == null || prereqId == null) {
            log.warn("IDs nulos | mallaMateriaId={} prerequisitoMallaMateriaId={}", materiaId, prereqId);
            throw new IllegalArgumentException("Los IDs de materia y prerrequisito son obligatorios.");
        }

        if (materiaId.equals(prereqId)) {
            log.warn("Auto-referencia | mallaMateriaId={}", materiaId);
            throw new IllegalArgumentException("Una materia no puede ser prerrequisito de sí misma.");
        }

        MallaMateria materia = mallaMateriaRepository.findById(materiaId)
                .orElseThrow(() -> {
                    log.warn("Materia no encontrada | mallaMateriaId={}", materiaId);
                    return new EntityNotFoundException("La materia con ID " + materiaId + " no existe.");
                });

        MallaMateria prerequisito = mallaMateriaRepository.findById(prereqId)
                .orElseThrow(() -> {
                    log.warn("Prerrequisito no encontrado | prerequisitoMallaMateriaId={}", prereqId);
                    return new EntityNotFoundException("El prerrequisito con ID " + prereqId + " no existe.");
                });

        String materiaNombre = materia.getMateria().getNombre();
        String prereqNombre = prerequisito.getMateria().getNombre();
        String materiaCodigo = materia.getMateria().getCodigo();
        String prereqCodigo = prerequisito.getMateria().getCodigo();

        if (!materia.getMalla().getId().equals(prerequisito.getMalla().getId())) {
            log.warn("Distinta malla | materiaId={} mallaMateria={} prereqMalla={}",
                    materiaId, materia.getMalla().getId(), prerequisito.getMalla().getId());
            throw new IllegalArgumentException(
                    "La materia \"" + prereqNombre + "\" no pertenece a la misma malla que \"" + materiaNombre + "\"."
            );
        }

        if (prerequisito.getSemestreSugerido() >= materia.getSemestreSugerido()) {
            log.warn("Semestre invalido | materiaSemestre={} prereqSemestre={}",
                    materia.getSemestreSugerido(), prerequisito.getSemestreSugerido());
            throw new IllegalArgumentException(
                    "La materia \"" + prereqNombre + "\" está en un semestre (" + prerequisito.getSemestreSugerido()
                    + ") que no es inferior al de \"" + materiaNombre + "\" (" + materia.getSemestreSugerido()
                    + "). El prerrequisito debe cursarse en un semestre anterior."
            );
        }

        if (prerequisitoRepository.existsByMallaMateria_IdAndPrerequisito_Id(materiaId, prereqId)) {
            log.warn("Prerrequisito duplicado | mallaMateriaId={} prerequisitoId={}", materiaId, prereqId);
            throw new IllegalArgumentException(
                    "La materia \"" + prereqNombre + "\" ya está registrada como prerrequisito de \"" + materiaNombre + "\"."
            );
        }

        List<Prerequisito> inversePair = prerequisitoRepository.findInversePair(materiaId, prereqId);
        if (!inversePair.isEmpty()) {
            log.warn("Relacion inversa | mallaMateriaId={} prerequisitoId={}", materiaId, prereqId);
            throw new IllegalArgumentException(
                    "No se puede crear una relación inversa: \"" + materiaNombre
                    + "\" ya es prerrequisito de \"" + prereqNombre + "\"."
            );
        }

        Prerequisito entity = new Prerequisito();
        entity.setMallaMateria(materia);
        entity.setPrerequisito(prerequisito);
        entity = prerequisitoRepository.save(entity);

        log.info("Prerrequisito creado exitosamente | id={} mallaMateriaId={} prerequisitoId={}",
                entity.getId(), materiaId, prereqId);

        return new PrerequisitoResponse(
                entity.getId(), materiaId, prereqId,
                materiaCodigo, materiaNombre,
                prereqCodigo, prereqNombre,
                materia.getSemestreSugerido(), prerequisito.getSemestreSugerido()
        );
    }

    @Transactional
    public void eliminarPrerequisito(Long mallaMateriaId, Long prerequisitoMallaMateriaId) {
        log.info("Eliminando prerrequisito | mallaMateriaId={} prerequisitoId={}", mallaMateriaId, prerequisitoMallaMateriaId);

        if (!prerequisitoRepository.existsByMallaMateria_IdAndPrerequisito_Id(mallaMateriaId, prerequisitoMallaMateriaId)) {
            log.warn("Prerrequisito no encontrado para eliminar | mallaMateriaId={} prerequisitoId={}",
                    mallaMateriaId, prerequisitoMallaMateriaId);
            throw new EntityNotFoundException(
                    "No existe la relación de prerrequisito entre la materia " + mallaMateriaId
                    + " y el prerrequisito " + prerequisitoMallaMateriaId + "."
            );
        }

        prerequisitoRepository.deleteByMallaMateria_IdAndPrerequisito_Id(mallaMateriaId, prerequisitoMallaMateriaId);
        log.info("Prerrequisito eliminado exitosamente | mallaMateriaId={} prerequisitoId={}",
                mallaMateriaId, prerequisitoMallaMateriaId);
    }
}
