package com.easyschedule.backend.academico.seleccion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.seleccion.model.Seleccion;
import com.easyschedule.backend.academico.seleccion.repository.SeleccionRepository;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeleccionServiceTest {

    @Mock
    private SeleccionRepository seleccionRepository;
    @Mock
    private OfertaMateriaRepository ofertaMateriaRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private MallaMateriaRepository mallaMateriaRepository;

    @InjectMocks
    private SeleccionService seleccionService;

    private Estudiante estudiante;
    private OfertaMateria ofertaMateria;
    private MallaMateria mallaMateria;
    private Materia materia;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setId(1L);

        materia = new Materia();
        materia.setId(10L);
        materia.setNombre("Matematica I");

        mallaMateria = new MallaMateria();
        mallaMateria.setId(100L);
        mallaMateria.setMateria(materia);

        ofertaMateria = new OfertaMateria();
        ofertaMateria.setId(1000L);
        ofertaMateria.setMallaMateriaId(100L);
        ofertaMateria.setParalelo("A");
    }

    @Test
    void addSelection_NewSelection_ShouldSave() {
        SeleccionRequest request = new SeleccionRequest();
        request.setOfertaMateriaId(1000L);

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(ofertaMateriaRepository.findById(1000L)).thenReturn(Optional.of(ofertaMateria));
        when(seleccionRepository.findByEstudianteIdAndMallaMateriaId(1L, 100L)).thenReturn(Optional.empty());
        when(mallaMateriaRepository.findById(100L)).thenReturn(Optional.of(mallaMateria));
        when(seleccionRepository.save(any(Seleccion.class))).thenAnswer(i -> i.getArguments()[0]);

        SeleccionResponse response = seleccionService.addSelection(1L, request);

        assertNotNull(response);
        assertEquals(1000L, response.getOfertaMateriaId());
        assertEquals("A", response.getParalelo());
        assertEquals("Matematica I", response.getMateriaNombre());
        verify(seleccionRepository).save(any(Seleccion.class));
    }

    @Test
    void addSelection_ReplaceParallel_ShouldUpdate() {
        SeleccionRequest request = new SeleccionRequest();
        request.setOfertaMateriaId(1001L); // New parallel

        OfertaMateria newOferta = new OfertaMateria();
        newOferta.setId(1001L);
        newOferta.setMallaMateriaId(100L);
        newOferta.setParalelo("B");

        Seleccion existingSeleccion = new Seleccion();
        existingSeleccion.setId(50L);
        existingSeleccion.setEstudiante(estudiante);
        existingSeleccion.setOfertaMateria(ofertaMateria); // Old parallel A

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(ofertaMateriaRepository.findById(1001L)).thenReturn(Optional.of(newOferta));
        when(seleccionRepository.findByEstudianteIdAndMallaMateriaId(1L, 100L)).thenReturn(Optional.of(existingSeleccion));
        when(mallaMateriaRepository.findById(100L)).thenReturn(Optional.of(mallaMateria));
        when(seleccionRepository.save(any(Seleccion.class))).thenAnswer(i -> i.getArguments()[0]);

        SeleccionResponse response = seleccionService.addSelection(1L, request);

        assertNotNull(response);
        assertEquals(1001L, response.getOfertaMateriaId());
        assertEquals("B", response.getParalelo());
        verify(seleccionRepository).save(existingSeleccion);
    }
}
