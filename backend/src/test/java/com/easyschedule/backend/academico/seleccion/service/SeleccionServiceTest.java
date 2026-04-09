package com.easyschedule.backend.academico.seleccion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.carrera.model.Carrera;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import com.easyschedule.backend.academico.malla.model.Malla;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.universidad.model.Universidad;
import com.easyschedule.backend.academico.universidad.repository.UniversidadRepository;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SeleccionServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private UniversidadRepository universidadRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @Mock
    private MallaRepository mallaRepository;

    @InjectMocks
    private SeleccionService seleccionService;

    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setId(10L);
    }

    @Test
    void getSeleccionByUserIdReturnsNullSelectionWhenProfileHasNoAcademicData() {
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));

        SeleccionResponse response = seleccionService.getSeleccionByUserId(10L);

        assertNull(response.universidadId());
        assertNull(response.carreraId());
        assertNull(response.mallaId());
    }

    @Test
    void saveSeleccionByUserIdPersistsSelectionWhenHierarchyIsValid() {
        SeleccionRequest request = new SeleccionRequest(1L, 11L, 101L);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getId()).thenReturn(1L);
        when(universidad.getNombre()).thenReturn("Universidad Catolica Boliviana");

        Carrera carrera = mock(Carrera.class);
        when(carrera.getId()).thenReturn(11L);
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");
        when(carrera.getUniversidadId()).thenReturn(1L);

        Malla malla = mock(Malla.class);
        when(malla.getId()).thenReturn(101L);
        when(malla.getNombre()).thenReturn("Malla 2017");
        when(malla.getCarreraId()).thenReturn(11L);

        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(mallaRepository.findByIdAndActiveTrue(101L)).thenReturn(Optional.of(malla));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));

        SeleccionResponse response = seleccionService.saveSeleccionByUserId(10L, request);

        assertEquals(1L, response.universidadId());
        assertEquals("Ingenieria de Sistemas", response.carrera());
        assertEquals("Malla 2017", response.malla());
        verify(estudianteRepository).save(estudiante);
    }

    @Test
    void saveSeleccionByUserIdResetsSemestreWhenMallaChanges() {
        SeleccionRequest request = new SeleccionRequest(1L, null, 101L);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getId()).thenReturn(1L);
        when(universidad.getNombre()).thenReturn("Universidad Catolica Boliviana");

        Carrera carrera = mock(Carrera.class);
        when(carrera.getId()).thenReturn(11L);
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");
        when(carrera.getUniversidadId()).thenReturn(1L);

        Malla malla = mock(Malla.class);
        when(malla.getId()).thenReturn(101L);
        when(malla.getNombre()).thenReturn("Malla 2017");
        when(malla.getCarreraId()).thenReturn(11L);

        Malla mallaAnterior = mock(Malla.class);
        when(mallaAnterior.getId()).thenReturn(88L);

        estudiante.setMalla(mallaAnterior);
        estudiante.setSemestreActual((short) 5);

        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(mallaRepository.findByIdAndActiveTrue(101L)).thenReturn(Optional.of(malla));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));

        seleccionService.saveSeleccionByUserId(10L, request);

        assertEquals(Short.valueOf((short) 1), estudiante.getSemestreActual());
        assertEquals(11L, estudiante.getCarreraId());
        assertEquals(1L, estudiante.getUniversidadId());
        verify(estudianteRepository).save(estudiante);
    }

    @Test
    void saveSeleccionByUserIdThrowsWhenCarreraDoesNotBelongToUniversidad() {
        SeleccionRequest request = new SeleccionRequest(1L, 11L, 101L);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getId()).thenReturn(1L);

        Carrera carrera = mock(Carrera.class);
        when(carrera.getUniversidadId()).thenReturn(2L);

        Malla malla = mock(Malla.class);
        when(malla.getCarreraId()).thenReturn(11L);

        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(mallaRepository.findByIdAndActiveTrue(101L)).thenReturn(Optional.of(malla));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> seleccionService.saveSeleccionByUserId(10L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void saveSeleccionByUserIdThrowsWhenMallaDoesNotBelongToUniversidad() {
        SeleccionRequest request = new SeleccionRequest(1L, null, 101L);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getId()).thenReturn(1L);

        Carrera carreraDeMalla = mock(Carrera.class);
        when(carreraDeMalla.getUniversidadId()).thenReturn(2L);

        Malla malla = mock(Malla.class);
        when(malla.getCarreraId()).thenReturn(11L);

        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(mallaRepository.findByIdAndActiveTrue(101L)).thenReturn(Optional.of(malla));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carreraDeMalla));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> seleccionService.saveSeleccionByUserId(10L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
