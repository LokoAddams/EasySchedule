package com.easyschedule.backend.academico.malla.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.estado_materia.service.EstadoMateriaService;
import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MallaServiceTest {

    @Mock
    private MallaRepository mallaRepository;

    @Mock
    private MallaMateriaRepository mallaMateriaRepository;

    @Mock
    private EstadoMateriaService estadoMateriaService;

    @Mock
    private PrerequisitoRepository prerequisitoRepository;

    @InjectMocks
    private MallaService mallaService;

    @Test
    void findMateriasByMallaMapsEstadoFromEstadoMateriaService() {
        Materia materia = new Materia();
        materia.setId(22L);
        materia.setCodigo("INF-101");
        materia.setNombre("Programacion I");

        MallaMateria mallaMateria = new MallaMateria();
        mallaMateria.setId(11L);
        mallaMateria.setMateria(materia);
        mallaMateria.setSemestreSugerido((short) 1);

        when(mallaMateriaRepository.findByMallaIdAndMateriaActiveTrueOrderBySemestreSugeridoAsc(99L))
            .thenReturn(List.of(mallaMateria));
        when(estadoMateriaService.getEstadoMateria(7L, 11L)).thenReturn("cursando");
        when(prerequisitoRepository.findByMallaMateria_Id(11L)).thenReturn(List.of());

        List<MallaMateriaResponse> result = mallaService.findMateriasByMalla(99L, 7L);

        assertEquals(1, result.size());
        assertEquals("INF-101", result.get(0).codigoMateria());
        assertEquals("cursando", result.get(0).estado());
        verify(estadoMateriaService).getEstadoMateria(7L, 11L);
    }
}
