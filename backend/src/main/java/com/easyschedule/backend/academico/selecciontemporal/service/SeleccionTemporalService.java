package com.easyschedule.backend.academico.selecciontemporal.service;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.selecciontemporal.dto.SeleccionTemporalRequest;
import com.easyschedule.backend.academico.selecciontemporal.dto.SeleccionTemporalResponse;
import com.easyschedule.backend.academico.selecciontemporal.model.SeleccionTemporal;
import com.easyschedule.backend.academico.selecciontemporal.model.SeleccionTemporal;
import com.easyschedule.backend.academico.selecciontemporal.repository.SeleccionTemporalRepository;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeleccionTemporalService {

    private final SeleccionTemporalRepository seleccionRepository;
    private final OfertaMateriaRepository ofertaMateriaRepository;
    private final EstudianteRepository estudianteRepository;
    private final MallaMateriaRepository mallaMateriaRepository;

    public SeleccionTemporalService(
            SeleccionTemporalRepository seleccionRepository,
            OfertaMateriaRepository ofertaMateriaRepository,
            EstudianteRepository estudianteRepository,
            MallaMateriaRepository mallaMateriaRepository) {
        this.seleccionRepository = seleccionRepository;
        this.ofertaMateriaRepository = ofertaMateriaRepository;
        this.estudianteRepository = estudianteRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
    }

    public List<SeleccionTemporalResponse> listByUserId(Long userId) {
        return seleccionRepository.findByEstudianteId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SeleccionTemporalResponse addSelection(Long userId, SeleccionTemporalRequest request) {
        Estudiante estudiante = estudianteRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        OfertaMateria oferta = ofertaMateriaRepository.findById(request.getOfertaMateriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta de materia no encontrada"));

        // Check if the subject (MallaMateria) is already selected with a different parallel
        Optional<SeleccionTemporal> existingSelection = seleccionRepository.findByEstudianteIdAndMallaMateriaId(userId, oferta.getMallaMateriaId());

        if (existingSelection.isPresent()) {
            SeleccionTemporal s = existingSelection.get();
            if (s.getOfertaMateria().getId().equals(oferta.getId())) {
                // Same parallel, no change needed (idempotent)
                return toResponse(s);
            } else {
                // Different parallel, replace it
                s.setOfertaMateria(oferta);
                s.setFechaSeleccion(OffsetDateTime.now());
                return toResponse(seleccionRepository.save(s));
            }
        }

        SeleccionTemporal nuevaSeleccion = new SeleccionTemporal();
        nuevaSeleccion.setEstudiante(estudiante);
        nuevaSeleccion.setOfertaMateria(oferta);
        nuevaSeleccion.setFechaSeleccion(OffsetDateTime.now());

        return toResponse(seleccionRepository.save(nuevaSeleccion));
    }

    @Transactional
    public void removeSelection(Long userId, Long ofertaMateriaId) {
        seleccionRepository.findByEstudianteIdAndOfertaMateriaId(userId, ofertaMateriaId)
                .ifPresent(seleccionRepository::delete);
    }

    @Transactional
    public void clearSelections(Long userId) {
        seleccionRepository.deleteByEstudianteId(userId);
    }

    private SeleccionTemporalResponse toResponse(SeleccionTemporal seleccion) {
        SeleccionTemporalResponse res = new SeleccionTemporalResponse();
        res.setId(seleccion.getId());
        
        OfertaMateria oferta = seleccion.getOfertaMateria();
        res.setOfertaMateriaId(oferta.getId());
        res.setParalelo(oferta.getParalelo());
        res.setDocente(oferta.getDocente());
        res.setHorarioJson(oferta.getHorarioJson());
        res.setAula(oferta.getAula());

        MallaMateria mm = mallaMateriaRepository.findById(oferta.getMallaMateriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Datos de malla inconsistentes"));
        
        res.setMateriaId(mm.getMateria().getId());
        res.setMateriaNombre(mm.getMateria().getNombre());
        
        return res;
    }
}
