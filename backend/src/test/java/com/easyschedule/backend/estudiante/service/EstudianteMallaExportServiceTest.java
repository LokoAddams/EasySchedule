package com.easyschedule.backend.estudiante.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.carrera.model.Carrera;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import com.easyschedule.backend.academico.malla.model.Malla;
import com.easyschedule.backend.academico.malla.service.MallaService;
import com.easyschedule.backend.academico.universidad.model.Universidad;
import com.easyschedule.backend.academico.universidad.repository.UniversidadRepository;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EstudianteMallaExportServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private MallaService mallaService;

    @Mock
    private UniversidadRepository universidadRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @InjectMocks
    private EstudianteMallaExportService exportService;

    @Test
    void exportarAvanceGraduacionGeneratesPdfWithCurrentSubjectStates() {
        Estudiante estudiante = buildEstudiante();
        Universidad universidad = mock(Universidad.class);
        Carrera carrera = mock(Carrera.class);

        when(universidad.getNombre()).thenReturn("UCB");
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(universidadRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(carrera));
        when(mallaService.findMateriasByMalla(30L, 1L)).thenReturn(List.of(
            new MallaMateriaResponse(100L, 200L, "SIS101", "Programacion I", (short) 5, (short) 1, "aprobada", List.of()),
            new MallaMateriaResponse(101L, 201L, "SIS102", "Programacion II", (short) 5, (short) 2, "cursando", List.of(100L)),
            new MallaMateriaResponse(102L, 202L, "SIS103", "Bases de Datos", (short) 5, (short) 3, null, List.of(101L))
        ));

        var export = exportService.exportarAvanceGraduacion(1L, "pdf");

        assertEquals("application/pdf", export.contentType());
        assertEquals("avance_graduacion.pdf", export.filename());
        assertTrue(new String(export.contenido(), 0, 4, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    }

    @Test
    void exportarAvanceGraduacionThrowsClearErrorWhenDataIsInsufficient() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(1L);
        estudiante.setUniversidadId(10L);
        estudiante.setCarreraId(20L);

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> exportService.exportarAvanceGraduacion(1L, "pdf")
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals("No existen datos suficientes para generar el reporte", ex.getReason());
    }

    @Test
    void exportarAvanceGraduacionRejectsUnsupportedFormats() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> exportService.exportarAvanceGraduacion(1L, "xlsx")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private Estudiante buildEstudiante() {
        Malla malla = new Malla();
        malla.setId(30L);
        malla.setNombre("Malla 2024");
        malla.setVersion("2024");

        Estudiante estudiante = new Estudiante();
        estudiante.setId(1L);
        estudiante.setUsername("diego");
        estudiante.setNombre("Diego");
        estudiante.setApellido("Suarez");
        estudiante.setCorreo("diego@mail.com");
        estudiante.setUniversidadId(10L);
        estudiante.setCarreraId(20L);
        estudiante.setMalla(malla);
        return estudiante;
    }
}
