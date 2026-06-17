package com.easyschedule.backend.academico.oferta_materia.service;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaDetalleResponse;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaListResponse;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaResponse;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaUpdateRequest;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaEdicionResponse;
import com.easyschedule.backend.academico.oferta_materia.dto.HorarioDto;
import java.time.LocalTime;
import java.util.List;

@Service
public class OfertaMateriaService {

    private final OfertaMateriaRepository ofertaMateriaRepository;
    private final MallaMateriaRepository mallaMateriaRepository;
    private final PrerequisitoRepository prerequisitoRepository;
    private final ObjectMapper objectMapper;

    public OfertaMateriaService(
        OfertaMateriaRepository ofertaMateriaRepository,
        MallaMateriaRepository mallaMateriaRepository,
        PrerequisitoRepository prerequisitoRepository,
        ObjectMapper objectMapper
    ) {
        this.ofertaMateriaRepository = ofertaMateriaRepository;
        this.mallaMateriaRepository = mallaMateriaRepository;
        this.prerequisitoRepository = prerequisitoRepository;
        this.objectMapper = objectMapper;
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

    @Transactional(readOnly = true)
    public List<OfertaMateriaListResponse> listarOfertas(
        Long mallaId,
        String search,
        String semestre,
        String paralelo
    ) {
        String searchValue = (search != null && !search.isBlank()) ? search.trim() : null;
        String semestreValue = (semestre != null && !semestre.isBlank()) ? semestre.trim() : null;
        String paraleloValue = (paralelo != null && !paralelo.isBlank()) ? paralelo.trim() : null;

        return ofertaMateriaRepository.findOfertasByFilters(mallaId, searchValue, semestreValue, paraleloValue)
            .stream()
            .map(row -> new OfertaMateriaListResponse(
                row.getId(),
                row.getCodigoMateria(),
                row.getNombreMateria(),
                row.getSemestre(),
                row.getParalelo(),
                row.getDocente(),
                row.getAula()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarSemestres(Long mallaId) {
        return ofertaMateriaRepository.findDistinctSemestresByMallaId(mallaId);
    }

    @Transactional(readOnly = true)
    public OfertaMateriaEdicionResponse obtenerParaEdicion(Long id) {
        OfertaMateria oferta = ofertaMateriaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
            
        MallaMateria mm = mallaMateriaRepository.findById(oferta.getMallaMateriaId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MallaMateria no encontrada"));
            
        List<HorarioDto> horarios;
        try {
            horarios = objectMapper.readValue(oferta.getHorarioJson(), new TypeReference<List<HorarioDto>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deserializando horarios");
        }
        
        return new OfertaMateriaEdicionResponse(
            oferta.getId(),
            mm.getMateria().getCodigo(),
            mm.getMateria().getNombre(),
            oferta.getParalelo(),
            oferta.getSemestre(),
            oferta.getDocente(),
            oferta.getAula(),
            horarios
        );
    }

    @Transactional(readOnly = true)
    public List<String> listarParalelos(Long mallaId) {
        return ofertaMateriaRepository.findDistinctParalelosByMallaId(mallaId);
    }

    @Transactional(readOnly = true)
    public void validarActualizacion(Long ofertaId, OfertaMateriaUpdateRequest request) {
        if (request.aula() == null || request.aula().isBlank()) {
            return; // If no classroom is assigned, no need to check collisions
        }
        
        List<OfertaMateria> posiblesChoques = ofertaMateriaRepository.findByAulaAndSemestreAndIdNot(
            request.aula(), request.semestre(), ofertaId
        );
        
        for (OfertaMateria otraOferta : posiblesChoques) {
            try {
                List<HorarioDto> otrosHorarios = objectMapper.readValue(
                    otraOferta.getHorarioJson(), 
                    new TypeReference<List<HorarioDto>>() {}
                );
                
                for (HorarioDto h1 : request.horarios()) {
                    for (HorarioDto h2 : otrosHorarios) {
                        if (h1.dia().equalsIgnoreCase(h2.dia())) {
                            LocalTime start1 = LocalTime.parse(h1.horaInicio());
                            LocalTime end1 = LocalTime.parse(h1.horaFin());
                            LocalTime start2 = LocalTime.parse(h2.horaInicio());
                            LocalTime end2 = LocalTime.parse(h2.horaFin());
                            
                            if (start1.isBefore(end2) && start2.isBefore(end1)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                                    "Choque de horario detectado en el aula " + request.aula() + 
                                    " el dia " + h1.dia() + " entre " + start1 + " - " + end1);
                            }
                        }
                    }
                }
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error validando horarios");
            }
        }
    }

    @Transactional
    public void actualizarOferta(Long ofertaId, OfertaMateriaUpdateRequest request) {
        validarActualizacion(ofertaId, request);
        
        OfertaMateria oferta = ofertaMateriaRepository.findById(ofertaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
            
        // Handle MallaMateria change if codigoMateria changed
        MallaMateria currentMm = mallaMateriaRepository.findById(oferta.getMallaMateriaId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MallaMateria actual no encontrada"));
            
        if (!currentMm.getMateria().getCodigo().equals(request.codigoMateria())) {
            MallaMateria newMm = mallaMateriaRepository.findByMallaIdAndMateria_Codigo(
                currentMm.getMalla().getId(), request.codigoMateria()
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Materia con codigo " + request.codigoMateria() + " no existe en esta malla"));
            
            oferta.setMallaMateriaId(newMm.getId());
        }
        
        oferta.setSemestre(request.semestre());
        oferta.setParalelo(request.paralelo());
        oferta.setDocente(request.docente());
        oferta.setAula(request.aula());
        
        try {
            oferta.setHorarioJson(objectMapper.writeValueAsString(request.horarios()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error serializando horario");
        }
        
        oferta.setFechaActualizacion(java.time.OffsetDateTime.now());
        ofertaMateriaRepository.save(oferta);
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
