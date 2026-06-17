package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaEditRequest;
import com.easyschedule.backend.academico.malla.dto.MallaEditResponse;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    if (prereq == null) {
                        logger.warn("Prerequisito con codigo {} no encontrado en la malla", prereqCodigo);
                        throw new IllegalArgumentException("La materia con codigo '" + prereqCodigo + "' no existe en la malla actual");
                    }
                    if (prereq.getId().equals(mallaMateria.getId())) {
                        throw new IllegalArgumentException("La materia '" + matReq.codigo() + "' no puede ser prerequisito de si misma");
                    }

                    // Check for inverse relationship
                    List<Prerequisito> inversePair = prerequisitoRepository.findInversePair(mallaMateria.getId(), prereq.getId());
                    if (!inversePair.isEmpty()) {
                        throw new IllegalArgumentException("Relacion inversa de prerequisito detectada entre '" +
                            matReq.codigo() + "' y '" + prereqCodigo + "'");
                    }

                    boolean exists = prerequisitoRepository.existsByMallaMateria_IdAndPrerequisito_Id(
                        mallaMateria.getId(), prereq.getId());
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

    @Transactional(readOnly = true)
    public MallaEditResponse getMallaEditable(Long mallaId) {
        Malla malla = findActiveMalla(mallaId);
        List<MateriaImportRequest> materias = mallaMateriaRepository.findByMallaIdOrderBySemestreSugeridoAsc(mallaId).stream()
            .map((mallaMateria) -> {
                List<String> prerequisitos = prerequisitoRepository.findByMallaMateria_Id(mallaMateria.getId()).stream()
                    .map((prerequisito) -> prerequisito.getPrerequisito().getMateria().getCodigo())
                    .toList();

                return new MateriaImportRequest(
                    mallaMateria.getMateria().getCodigo(),
                    mallaMateria.getMateria().getNombre(),
                    Integer.valueOf(mallaMateria.getSemestreSugerido()),
                    Integer.valueOf(mallaMateria.getMateria().getCreditos()),
                    prerequisitos
                );
            })
            .toList();

        return new MallaEditResponse(malla.getId(), malla.getNombre(), malla.getVersion(), malla.getCarreraId(), materias);
    }

    @Transactional
    public MallaEditResponse actualizarMalla(Long mallaId, MallaEditRequest request) {
        Malla malla = findActiveMalla(mallaId);
        List<MateriaImportRequest> materias = validateMateriasForEdit(request);

        List<MallaMateria> currentRows = mallaMateriaRepository.findByMallaIdOrderBySemestreSugeridoAsc(mallaId);
        Map<String, MallaMateria> currentByCode = new HashMap<>();
        for (MallaMateria current : currentRows) {
            currentByCode.put(normalizeCode(current.getMateria().getCodigo()), current);
        }

        List<Prerequisito> currentPrerequisitos = prerequisitoRepository
            .findByMallaMateria_Malla_IdOrPrerequisito_Malla_Id(mallaId, mallaId);
        prerequisitoRepository.deleteAll(currentPrerequisitos);
        prerequisitoRepository.flush();

        Map<String, MallaMateria> updatedByCode = new LinkedHashMap<>();
        Set<Long> keptMallaMateriaIds = new HashSet<>();

        for (MateriaImportRequest materiaRequest : materias) {
            String normalizedCode = normalizeCode(materiaRequest.codigo());
            Materia materia = materiaRepository.findByCodigoIgnoreCase(normalizedCode)
                .map((existing) -> updateMateria(existing, materiaRequest))
                .orElseGet(() -> createMateria(materiaRequest));

            MallaMateria mallaMateria = currentByCode.get(normalizedCode);
            if (mallaMateria == null) {
                mallaMateria = new MallaMateria();
                mallaMateria.setMalla(malla);
                mallaMateria.setMateria(materia);
            } else {
                mallaMateria.setMateria(materia);
            }

            mallaMateria.setSemestreSugerido(materiaRequest.semestre().shortValue());
            mallaMateria = mallaMateriaRepository.save(mallaMateria);
            updatedByCode.put(normalizedCode, mallaMateria);
            keptMallaMateriaIds.add(mallaMateria.getId());
        }

        for (MallaMateria current : currentRows) {
            if (!keptMallaMateriaIds.contains(current.getId())) {
                mallaMateriaRepository.delete(current);
            }
        }

        for (MateriaImportRequest materiaRequest : materias) {
            MallaMateria mallaMateria = updatedByCode.get(normalizeCode(materiaRequest.codigo()));
            if (materiaRequest.prerequisitos() == null) {
                continue;
            }

            for (String prerequisitoCode : materiaRequest.prerequisitos()) {
                MallaMateria prerequisito = updatedByCode.get(normalizeCode(prerequisitoCode));
                Prerequisito entity = new Prerequisito();
                entity.setMallaMateria(mallaMateria);
                entity.setPrerequisito(prerequisito);
                prerequisitoRepository.save(entity);
            }
        }

        return getMallaEditable(mallaId);
    }

    private Malla findActiveMalla(Long mallaId) {
        if (mallaId == null) {
            throw new IllegalArgumentException("El ID de la malla es requerido");
        }

        return mallaRepository.findByIdAndActiveTrue(mallaId)
            .orElseThrow(() -> new IllegalArgumentException("La malla seleccionada no existe"));
    }

    private List<MateriaImportRequest> validateMateriasForEdit(MallaEditRequest request) {
        if (request == null || request.materias() == null || request.materias().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos una materia");
        }

        Map<String, MateriaImportRequest> byCode = new LinkedHashMap<>();
        for (MateriaImportRequest materia : request.materias()) {
            validateMateria(materia);
            String normalizedCode = normalizeCode(materia.codigo());
            if (byCode.containsKey(normalizedCode)) {
                throw new IllegalArgumentException("El codigo de materia '" + materia.codigo() + "' esta duplicado");
            }
            byCode.put(normalizedCode, normalizeMateria(materia));
        }

        for (MateriaImportRequest materia : byCode.values()) {
            validatePrerequisitos(materia, byCode);
        }

        validateNoCycles(byCode);
        return new ArrayList<>(byCode.values());
    }

    private void validateMateria(MateriaImportRequest materia) {
        if (materia == null) {
            throw new IllegalArgumentException("La materia es requerida");
        }
        if (materia.codigo() == null || materia.codigo().isBlank()) {
            throw new IllegalArgumentException("El codigo de la materia es requerido");
        }
        if (materia.nombre() == null || materia.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la materia es requerido");
        }
        if (materia.semestre() == null || materia.semestre() < 1 || materia.semestre() > 50) {
            throw new IllegalArgumentException("El semestre sugerido debe estar entre 1 y 50");
        }
        if (materia.creditos() == null || materia.creditos() < 1 || materia.creditos() > 99) {
            throw new IllegalArgumentException("Los creditos deben estar entre 1 y 99");
        }
    }

    private MateriaImportRequest normalizeMateria(MateriaImportRequest materia) {
        List<String> prerequisitos = materia.prerequisitos() == null
            ? List.of()
            : materia.prerequisitos().stream()
                .map(this::normalizeCode)
                .filter((code) -> !code.isBlank())
                .distinct()
                .toList();

        return new MateriaImportRequest(
            normalizeCode(materia.codigo()),
            materia.nombre().trim(),
            materia.semestre(),
            materia.creditos(),
            prerequisitos
        );
    }

    private void validatePrerequisitos(MateriaImportRequest materia, Map<String, MateriaImportRequest> byCode) {
        for (String prerequisitoCode : materia.prerequisitos()) {
            MateriaImportRequest prerequisito = byCode.get(normalizeCode(prerequisitoCode));
            if (prerequisito == null) {
                throw new IllegalArgumentException("El prerequisito '" + prerequisitoCode + "' no existe en la malla");
            }
            if (normalizeCode(materia.codigo()).equals(normalizeCode(prerequisitoCode))) {
                throw new IllegalArgumentException("Una materia no puede ser prerequisito de si misma");
            }
            if (prerequisito.semestre() >= materia.semestre()) {
                throw new IllegalArgumentException("Los prerequisitos deben pertenecer a semestres anteriores");
            }
        }
    }

    private void validateNoCycles(Map<String, MateriaImportRequest> byCode) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (String code : byCode.keySet()) {
            detectCycle(code, byCode, visiting, visited);
        }
    }

    private void detectCycle(
        String code,
        Map<String, MateriaImportRequest> byCode,
        Set<String> visiting,
        Set<String> visited
    ) {
        if (visited.contains(code)) {
            return;
        }
        if (!visiting.add(code)) {
            throw new IllegalArgumentException("La malla contiene un ciclo de prerequisitos");
        }

        MateriaImportRequest materia = byCode.get(code);
        for (String prerequisito : materia.prerequisitos()) {
            detectCycle(normalizeCode(prerequisito), byCode, visiting, visited);
        }

        visiting.remove(code);
        visited.add(code);
    }

    private Materia updateMateria(Materia materia, MateriaImportRequest request) {
        materia.setCodigo(normalizeCode(request.codigo()));
        materia.setNombre(request.nombre().trim());
        materia.setCreditos(request.creditos().shortValue());
        materia.setActive(true);
        return materiaRepository.save(materia);
    }

    private Materia createMateria(MateriaImportRequest request) {
        Materia materia = new Materia();
        materia.setCodigo(normalizeCode(request.codigo()));
        materia.setNombre(request.nombre().trim());
        materia.setCreditos(request.creditos().shortValue());
        materia.setActive(true);
        return materiaRepository.save(materia);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }
}
