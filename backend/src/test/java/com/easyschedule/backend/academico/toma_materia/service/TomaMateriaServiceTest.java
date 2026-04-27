package com.easyschedule.backend.academico.toma_materia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.estado_materia.service.EstadoMateriaService;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.model.Prerequisito;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaRequest;
import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaResponse;
import com.easyschedule.backend.academico.toma_materia.model.TomaMateriaEstudiante;
import com.easyschedule.backend.academico.toma_materia.repository.TomaMateriaEstudianteRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TomaMateriaServiceTest {

    @Mock
    private TomaMateriaEstudianteRepository tomaMateriaEstudianteRepository;
    @Mock
    private OfertaMateriaRepository ofertaMateriaRepository;
    @Mock
    private EstadoMateriaService estadoMateriaService;
    @Mock
    private PrerequisitoRepository prerequisitoRepository;
    @Mock
    private MallaMateriaRepository mallaMateriaRepository;

    @InjectMocks
    private TomaMateriaService tomaMateriaService;

    @Test
    void saveByUserId_ThrowsWhenMallaNotFound() {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setMallaMateriaId(99L);
        when(ofertaMateriaRepository.findAllById(List.of(99L))).thenReturn(List.of(oferta));
        when(mallaMateriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> 
            tomaMateriaService.saveByUserId(5L, new TomaMateriaRequest(List.of(99L))));
    }

    @Test
    void saveByUserId_ThrowsWhenMateriaYaAprobada() {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setMallaMateriaId(77L);
        
        Materia materia = new Materia();
        materia.setCreditos((short) 5);
        materia.setNombre("Matematicas");
        
        MallaMateria mm = new MallaMateria();
        mm.setId(77L);
        mm.setMateria(materia);

        when(ofertaMateriaRepository.findAllById(List.of(77L))).thenReturn(List.of(oferta));
        when(mallaMateriaRepository.findById(77L)).thenReturn(Optional.of(mm));
        when(estadoMateriaService.getEstadoMateria(5L, 77L)).thenReturn("aprobada");

        assertThrows(ResponseStatusException.class, () -> 
            tomaMateriaService.saveByUserId(5L, new TomaMateriaRequest(List.of(77L))));
    }

    @Test
    void saveByUserId_ThrowsWhenFaltaPrerequisito() {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setMallaMateriaId(77L);
        
        Materia materia = new Materia();
        materia.setCreditos((short) 5);
        materia.setNombre("Fisica");
        
        MallaMateria mm = new MallaMateria();
        mm.setId(77L);
        mm.setMateria(materia);

        Materia materiaPre = new Materia();
        materiaPre.setNombre("Matematicas");
        MallaMateria mmPre = new MallaMateria();
        mmPre.setId(10L);
        mmPre.setMateria(materiaPre);
        
        Prerequisito prerequisito = new Prerequisito();
        prerequisito.setPrerequisito(mmPre);

        when(ofertaMateriaRepository.findAllById(List.of(77L))).thenReturn(List.of(oferta));
        when(mallaMateriaRepository.findById(77L)).thenReturn(Optional.of(mm));
        when(estadoMateriaService.getEstadoMateria(5L, 77L)).thenReturn("pendiente");
        when(prerequisitoRepository.findByMallaMateria_Id(77L)).thenReturn(List.of(prerequisito));
        when(estadoMateriaService.getEstadoMateria(5L, 10L)).thenReturn("reprobada");

        assertThrows(ResponseStatusException.class, () -> 
            tomaMateriaService.saveByUserId(5L, new TomaMateriaRequest(List.of(77L))));
    }

    @Test
    void saveByUserId_ThrowsWhenExcedeCreditos() {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setMallaMateriaId(77L);
        
        Materia materia = new Materia();
        materia.setCreditos((short) 35);
        
        MallaMateria mm = new MallaMateria();
        mm.setId(77L);
        mm.setMateria(materia);

        when(ofertaMateriaRepository.findAllById(List.of(77L))).thenReturn(List.of(oferta));
        when(mallaMateriaRepository.findById(77L)).thenReturn(Optional.of(mm));
        when(estadoMateriaService.getEstadoMateria(5L, 77L)).thenReturn("pendiente");
        when(prerequisitoRepository.findByMallaMateria_Id(77L)).thenReturn(Collections.emptyList());

        assertThrows(ResponseStatusException.class, () -> 
            tomaMateriaService.saveByUserId(5L, new TomaMateriaRequest(List.of(77L))));
    }

    @Test
    void saveByUserId_Success() {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setId(100L);
        oferta.setMallaMateriaId(77L);
        
        Materia materia = new Materia();
        materia.setCreditos((short) 5);
        
        MallaMateria mm = new MallaMateria();
        mm.setId(77L);
        mm.setMateria(materia);

        when(ofertaMateriaRepository.findAllById(List.of(100L))).thenReturn(List.of(oferta));
        when(mallaMateriaRepository.findById(77L)).thenReturn(Optional.of(mm));
        when(estadoMateriaService.getEstadoMateria(5L, 77L)).thenReturn("pendiente");
        when(prerequisitoRepository.findByMallaMateria_Id(77L)).thenReturn(Collections.emptyList());

        TomaMateriaEstudiante saved = new TomaMateriaEstudiante();
        saved.setId(1L);
        saved.setUserId(5L);
        saved.setOfertaId(100L);
        saved.setEstado("inscrita");
        
        when(tomaMateriaEstudianteRepository.save(any(TomaMateriaEstudiante.class))).thenReturn(saved);

        List<TomaMateriaResponse> result = tomaMateriaService.saveByUserId(5L, new TomaMateriaRequest(List.of(100L)));

        assertEquals(1, result.size());
        assertEquals("inscrita", result.get(0).estado());
        verify(estadoMateriaService).syncEstadoFromToma(5L, 77L, "inscrita");
    }

    @Test
    void deleteByUserIdAndOfertaId() {
        TomaMateriaEstudiante toma = new TomaMateriaEstudiante();
        when(tomaMateriaEstudianteRepository.findByUserIdAndOfertaId(3L, 77L)).thenReturn(Optional.of(toma));

        tomaMateriaService.deleteByUserIdAndOfertaId(3L, 77L);

        verify(tomaMateriaEstudianteRepository).delete(toma);
    }
}
