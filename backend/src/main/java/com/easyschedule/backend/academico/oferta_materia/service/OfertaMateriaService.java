package com.easyschedule.backend.academico.oferta_materia.service;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaDetalleResponse;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaResponse;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class OfertaMateriaService {

    private final OfertaMateriaRepository ofertaMateriaRepository;
    private final MallaMateriaRepository mallaMateriaRepository;
    private final PrerequisitoRepository prerequisitoRepository;

    public OfertaMateriaService(
        OfertaMateriaRepository ofertaMateriaRepository,
        MallaMateriaRepository mallaMateriaRepository,
        PrerequisitoRepository prerequisitoRepository
    ) {
        this.ofertaMateriaRepository = ofertaMateriaRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
        this.prerequisitoRepository = prerequisitoRepository;
    }

    @Transactional(readOnly = true)
    public OfertaDetalleResponse getDetalleParaInscripcion(Long mallaMateriaId) {
        MallaMateria mm = mallaMateriaRepository.findById(mallaMateriaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MallaMateria no encontrada"));

        List<OfertaMateriaResponse> ofertas = ofertaMateriaRepository.findByMallaMateriaId(mallaMateriaId)
            .stream()
            .map(this::toResponse)
            .toList();

        List<String> prerequisitosNombres = prerequisitoRepository.findByMallaMateria_Id(mallaMateriaId)
            .stream()
            .map(p -> p.getPrerequisito().getMateria().getNombre())
            .toList();

        return new OfertaDetalleResponse(
            mm.getId(),
            mm.getMateria().getNombre(),
            mm.getMateria().getCreditos(),
            prerequisitosNombres,
            ofertas
        );
    }

    private OfertaMateriaResponse toResponse(OfertaMateria o) {
        return new OfertaMateriaResponse(
            o.getId(),
            o.getSemestre(),
            o.getParalelo(),
            o.getDocente(),
            o.getAula()
        );
    }
}
