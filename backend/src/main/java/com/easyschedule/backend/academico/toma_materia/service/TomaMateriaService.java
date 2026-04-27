package com.easyschedule.backend.academico.toma_materia.service;

import com.easyschedule.backend.academico.estado_materia.service.EstadoMateriaService;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.model.Prerequisito;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaRequest;
import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaResponse;
import com.easyschedule.backend.academico.toma_materia.model.TomaMateriaEstudiante;
import com.easyschedule.backend.academico.toma_materia.repository.TomaMateriaEstudianteRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TomaMateriaService {

    private static final int MAX_CREDITOS_SEMESTRE = 30;

    private final TomaMateriaEstudianteRepository tomaMateriaEstudianteRepository;
    private final OfertaMateriaRepository ofertaMateriaRepository;
    private final EstadoMateriaService estadoMateriaService;
    private final PrerequisitoRepository prerequisitoRepository;
    private final MallaMateriaRepository mallaMateriaRepository;

    public TomaMateriaService(
        TomaMateriaEstudianteRepository tomaMateriaEstudianteRepository,
        OfertaMateriaRepository ofertaMateriaRepository,
        EstadoMateriaService estadoMateriaService,
        PrerequisitoRepository prerequisitoRepository,
        MallaMateriaRepository mallaMateriaRepository
    ) {
        this.tomaMateriaEstudianteRepository = tomaMateriaEstudianteRepository;
        this.ofertaMateriaRepository = ofertaMateriaRepository;
        this.estadoMateriaService = estadoMateriaService;
        this.prerequisitoRepository = prerequisitoRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
    }

    public List<TomaMateriaResponse> listByUserId(Long userId) {
        return tomaMateriaEstudianteRepository.findByUserIdOrderByFechaInscripcionDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<TomaMateriaResponse> saveByUserId(Long userId, TomaMateriaRequest request) {
        List<OfertaMateria> ofertas = ofertaMateriaRepository.findAllById(request.ofertaIds());
        int totalCreditos = 0;

        for (OfertaMateria oferta : ofertas) {
            MallaMateria mm = mallaMateriaRepository.findById(oferta.getMallaMateriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Malla no encontrada"));
            
            totalCreditos += mm.getMateria().getCreditos();

            String estado = estadoMateriaService.getEstadoMateria(userId, mm.getId());
            if ("aprobada".equalsIgnoreCase(estado) || "cursando".equalsIgnoreCase(estado)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes aprobada o en curso: " + mm.getMateria().getNombre());
            }

            List<Prerequisito> pre = prerequisitoRepository.findByMallaMateria_Id(mm.getId());
            for (Prerequisito p : pre) {
                String estadoPre = estadoMateriaService.getEstadoMateria(userId, p.getPrerequisito().getId());
                if (!"aprobada".equalsIgnoreCase(estadoPre)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta prerrequisito: " + p.getPrerequisito().getMateria().getNombre());
                }
            }
        }

        if (totalCreditos > MAX_CREDITOS_SEMESTRE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Superas el limite de " + MAX_CREDITOS_SEMESTRE + " creditos");
        }

        List<TomaMateriaResponse> res = new ArrayList<>();
        for (OfertaMateria o : ofertas) {
            TomaMateriaEstudiante t = new TomaMateriaEstudiante();
            t.setUserId(userId);
            t.setOfertaId(o.getId());
            t.setEstado("inscrita");
            t.setFechaInscripcion(OffsetDateTime.now());
            t.setFechaActualizacion(OffsetDateTime.now());
            
            TomaMateriaEstudiante saved = tomaMateriaEstudianteRepository.save(t);
            estadoMateriaService.syncEstadoFromToma(userId, o.getMallaMateriaId(), "inscrita");
            res.add(toResponse(saved));
        }
        return res;
    }

    @Transactional
    public void deleteByUserIdAndOfertaId(Long userId, Long ofertaId) {
        tomaMateriaEstudianteRepository.findByUserIdAndOfertaId(userId, ofertaId)
            .ifPresent(tomaMateriaEstudianteRepository::delete);
    }

    private TomaMateriaResponse toResponse(TomaMateriaEstudiante toma) {
        return new TomaMateriaResponse(toma.getId(), toma.getUserId(), toma.getOfertaId(), toma.getEstado(), toma.getFechaInscripcion(), toma.getFechaActualizacion());
    }
}
