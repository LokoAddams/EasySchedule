package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaImportRequest;
import com.easyschedule.backend.academico.malla.dto.MallaImportResponse;
import com.easyschedule.backend.academico.malla.dto.MateriaImportRequest;
import com.easyschedule.backend.academico.malla.model.Malla;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.model.Prerequisito;
import com.easyschedule.backend.academico.materia.repository.MateriaRepository;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class MallaImportService {

    private static final Logger logger = LoggerFactory.getLogger(MallaImportService.class);

    private final MallaRepository mallaRepository;
    private final MateriaRepository materiaRepository;
    private final MallaMateriaRepository mallaMateriaRepository;
    private final PrerequisitoRepository prerequisitoRepository;
    private final CarreraRepository carreraRepository;

    public MallaImportService(MallaRepository mallaRepository,
                              MateriaRepository materiaRepository,
                              MallaMateriaRepository mallaMateriaRepository,
                              PrerequisitoRepository prerequisitoRepository,
                              CarreraRepository carreraRepository) {
        this.mallaRepository = mallaRepository;
        this.materiaRepository = materiaRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
        this.prerequisitoRepository = prerequisitoRepository;
        this.carreraRepository = carreraRepository;
    }

    @Transactional
    public MallaImportResponse importarMalla(MallaImportRequest request) {
        logger.info("Iniciando importación de malla: nombre={}, carreraId={}, totalMaterias={}",
            request.nombre(), request.carreraId(), request.materias() != null ? request.materias().size() : 0);

        if (request.nombre() == null || request.nombre().isBlank()) {
            logger.warn("Error de validación: nombre de malla requerido");
            throw new IllegalArgumentException("El nombre de la malla es requerido");
        }
        if (request.carreraId() == null) {
            logger.warn("Error de validación: carreraId requerido");
            throw new IllegalArgumentException("El ID de la carrera es requerido");
        }
        if (request.materias() == null || request.materias().isEmpty()) {
            logger.warn("Error de validación: no se proporcionaron materias");
            throw new IllegalArgumentException("Debe proporcionar al menos una materia");
        }

        // Validar que la carrera exista
        if (!carreraRepository.existsById(request.carreraId())) {
            logger.warn("Error: carrera con id={} no existe", request.carreraId());
            throw new IllegalArgumentException("La carrera con ID " + request.carreraId() + " no existe");
        }

        // Validar que no exista una malla activa con la misma carrera y versión
        String version = request.version() != null ? request.version() : "1.0";
        if (mallaRepository.existsByCarreraIdAndVersionAndActiveTrue(request.carreraId(), version)) {
            logger.warn("Error: ya existe una malla activa para carreraId={} con versión {}", request.carreraId(), version);
            throw new IllegalArgumentException("Ya existe una malla activa para esta carrera con versión " + version + ". Use una versión diferente.");
        }

        Malla malla = new Malla();
        malla.setNombre(request.nombre());
        malla.setVersion(request.version() != null ? request.version() : "1.0");
        malla.setCarreraId(request.carreraId());
        malla.setActive(true);
        malla = mallaRepository.save(malla);
        logger.info("Malla creada: id={}, nombre={}, version={}", malla.getId(), malla.getNombre(), malla.getVersion());

        Map<String, MallaMateria> materiasMap = new HashMap<>();
        int prerequisitosCount = 0;

        for (MateriaImportRequest matReq : request.materias()) {
            if (matReq.codigo() == null || matReq.codigo().isBlank()) {
                logger.warn("Error de validación: código de materia requerido");
                throw new IllegalArgumentException("El código de la materia es requerido");
            }
            if (matReq.nombre() == null || matReq.nombre().isBlank()) {
                logger.warn("Error de validación: nombre de materia requerido para código={}", matReq.codigo());
                throw new IllegalArgumentException("El nombre de la materia es requerido");
            }
            if (matReq.semestre() == null || matReq.semestre() < 1) {
                logger.warn("Error de validación: semestre inválido para materia código={}", matReq.codigo());
                throw new IllegalArgumentException("El semestre sugerido debe ser mayor a 0");
            }

            Materia materia = materiaRepository.findByCodigo(matReq.codigo())
                .orElseGet(() -> {
                    Materia nueva = new Materia();
                    nueva.setCodigo(matReq.codigo());
                    nueva.setNombre(matReq.nombre());
                    nueva.setCreditos(matReq.creditos() != null ? matReq.creditos().shortValue() : 0);
                    nueva.setActive(true);
                    logger.info("Nueva materia creada: codigo={}, nombre={}", matReq.codigo(), matReq.nombre());
                    return materiaRepository.save(nueva);
                });

            if (!materia.getNombre().equals(matReq.nombre()) && matReq.nombre() != null) {
                materia.setNombre(matReq.nombre());
                materiaRepository.save(materia);
                logger.info("Nombre de materia actualizado: codigo={}, nuevoNombre={}", matReq.codigo(), matReq.nombre());
            }

            MallaMateria mallaMateria = new MallaMateria();
            mallaMateria.setMalla(malla);
            mallaMateria.setMateria(materia);
            mallaMateria.setSemestreSugerido(matReq.semestre().shortValue());
            mallaMateria = mallaMateriaRepository.save(mallaMateria);
            logger.debug("MallaMateria guardada: id={}, materiaId={}, semestre={}", mallaMateria.getId(), materia.getId(), matReq.semestre());

            materiasMap.put(matReq.codigo(), mallaMateria);
        }

        logger.info("Procesando prerequisitos para {} materias", request.materias().size());
        for (MateriaImportRequest matReq : request.materias()) {
            if (matReq.prerequisitos() != null && !matReq.prerequisitos().isEmpty()) {
                MallaMateria mallaMateria = materiasMap.get(matReq.codigo());
                for (String prereqCodigo : matReq.prerequisitos()) {
                    MallaMateria prereq = materiasMap.get(prereqCodigo);
                    if (prereq != null && !prereq.getId().equals(mallaMateria.getId())) {
                        boolean exists = prerequisitoRepository.findByMallaMateria_Id(mallaMateria.getId())
                            .stream()
                            .anyMatch(p -> p.getPrerequisito().getId().equals(prereq.getId()));
                        if (!exists) {
                            Prerequisito pre = new Prerequisito();
                            pre.setMallaMateria(mallaMateria);
                            pre.setPrerequisito(prereq);
                            prerequisitoRepository.save(pre);
                            prerequisitosCount++;
                            logger.debug("Prerequisito creado: materia={} requiere {}", matReq.codigo(), prereqCodigo);
                        } else {
                            logger.debug("Prerequisito ya existente: materia={} requiere {}", matReq.codigo(), prereqCodigo);
                        }
                    } else {
                        logger.warn("Prerequisito no encontrado o es la misma materia: materia={}, prerequisito={}", matReq.codigo(), prereqCodigo);
                    }
                }
            }
        }

        logger.info("Importación finalizada exitosamente: mallaId={}, materiasImportadas={}, prerequisitosImportados={}",
            malla.getId(), request.materias().size(), prerequisitosCount);

        return new MallaImportResponse(
            malla.getId(),
            malla.getNombre(),
            request.materias().size(),
            prerequisitosCount,
            "Malla importada exitosamente"
        );
    }
}
