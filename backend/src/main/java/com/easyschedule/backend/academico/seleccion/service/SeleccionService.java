package com.easyschedule.backend.academico.seleccion.service;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.seleccion.model.Seleccion;
import com.easyschedule.backend.academico.seleccion.repository.SeleccionRepository;
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
public class SeleccionService {

    private final SeleccionRepository seleccionRepository;
    private final OfertaMateriaRepository ofertaMateriaRepository;
    private final EstudianteRepository estudianteRepository;
    private final MallaMateriaRepository mallaMateriaRepository;

    public SeleccionService(
            SeleccionRepository seleccionRepository,
            OfertaMateriaRepository ofertaMateriaRepository,
            EstudianteRepository estudianteRepository,
            MallaMateriaRepository mallaMateriaRepository) {
        this.seleccionRepository = seleccionRepository;
        this.ofertaMateriaRepository = ofertaMateriaRepository;
        this.estudianteRepository = estudianteRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
    }

    public List<SeleccionResponse> listByUserId(Long userId) {
        return seleccionRepository.findByEstudianteId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SeleccionResponse addSelection(Long userId, SeleccionRequest request) {
        Estudiante estudiante = estudianteRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        OfertaMateria oferta = ofertaMateriaRepository.findById(request.getOfertaMateriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta de materia no encontrada"));

        // Check if the subject (MallaMateria) is already selected with a different parallel
        Optional<Seleccion> existingSelection = seleccionRepository.findByEstudianteIdAndMallaMateriaId(userId, oferta.getMallaMateriaId());

        if (existingSelection.isPresent()) {
            Seleccion s = existingSelection.get();
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

        Seleccion nuevaSeleccion = new Seleccion();
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

    private SeleccionResponse toResponse(Seleccion seleccion) {
        SeleccionResponse res = new SeleccionResponse();
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
