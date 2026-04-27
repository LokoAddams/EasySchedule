package com.easyschedule.backend.academico.horario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.carrera.model.Carrera;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import com.easyschedule.backend.academico.horario.dto.HorarioActualResponse;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.universidad.model.Universidad;
import com.easyschedule.backend.academico.universidad.repository.UniversidadRepository;
import com.easyschedule.backend.academico.malla.model.Malla;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HorarioRecomendadoServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private UniversidadRepository universidadRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @Mock
    private OfertaMateriaRepository ofertaMateriaRepository;

    @Test
    void getHorarioActualByUserIdParsesHorarioJsonRows() {
        HorarioRecomendadoService service = new HorarioRecomendadoService(
            estudianteRepository,
            universidadRepository,
            carreraRepository,
            ofertaMateriaRepository,
            new ObjectMapper()
        );

        Estudiante estudiante = new Estudiante();
        estudiante.setId(7L);
        estudiante.setSemestreActual((short) 1);
        estudiante.setUniversidadId(1L);
        estudiante.setCarreraId(11L);

        Malla malla = new Malla();
        malla.setId(16L);
        malla.setNombre("Malla 2024");
        estudiante.setMalla(malla);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getNombre()).thenReturn("Universidad Catolica Boliviana");

        Carrera carrera = mock(Carrera.class);
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");

        OfertaMateriaRepository.HorarioOfertaRow row = mock(OfertaMateriaRepository.HorarioOfertaRow.class);
        when(row.getSemestre()).thenReturn("2026-1");
        when(row.getParalelo()).thenReturn("A");
        when(row.getMateriaNombre()).thenReturn("Materia SIS S1 M1");
        when(row.getHorarioJson()).thenReturn("[{\"dia\":\"Lunes\",\"inicio\":\"07:00\",\"fin\":\"08:30\"}]");
        when(row.getDocente()).thenReturn("Docente");
        when(row.getAula()).thenReturn("A-01-1");

        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(ofertaMateriaRepository.findHorarioActualRows(7L, 16L, (short) 1)).thenReturn(List.of(row));

        HorarioActualResponse response = service.getHorarioActualByUserId(7L);

        assertNotNull(response);
        assertEquals("Ingenieria de Sistemas", response.carrera());
        assertEquals("2026-1", response.semestreOferta());
        assertEquals(1, response.clases().size());
        assertEquals("Lunes", response.clases().get(0).dia());
        assertEquals("07:00", response.clases().get(0).horaInicio());
    }

    @Test
    void getHorarioActualByUserIdReturnsEmptyWhenStudentNotFound() {
        HorarioRecomendadoService service = new HorarioRecomendadoService(
            estudianteRepository,
            universidadRepository,
            carreraRepository,
            ofertaMateriaRepository,
            new ObjectMapper()
        );

        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        HorarioActualResponse response = service.getHorarioActualByUserId(99L);

        assertNotNull(response);
        assertNull(response.universidad());
        assertNull(response.carrera());
        assertNull(response.malla());
        assertNull(response.semestreOferta());
        assertNull(response.semestreActual());
        assertTrue(response.clases().isEmpty());
    }

    @Test
    void buildHorarioActualCsvIncludesMetadataAndClasses() {
        HorarioRecomendadoService service = new HorarioRecomendadoService(
            estudianteRepository,
            universidadRepository,
            carreraRepository,
            ofertaMateriaRepository,
            new ObjectMapper()
        );

        Estudiante estudiante = new Estudiante();
        estudiante.setId(7L);
        estudiante.setSemestreActual((short) 1);
        estudiante.setUniversidadId(1L);
        estudiante.setCarreraId(11L);

        Malla malla = new Malla();
        malla.setId(16L);
        malla.setNombre("Malla 2024");
        estudiante.setMalla(malla);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getNombre()).thenReturn("Universidad Catolica Boliviana");

        Carrera carrera = mock(Carrera.class);
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");

        OfertaMateriaRepository.HorarioOfertaRow row = mock(OfertaMateriaRepository.HorarioOfertaRow.class);
        when(row.getSemestre()).thenReturn("2026-1");
        when(row.getParalelo()).thenReturn("A");
        when(row.getMateriaNombre()).thenReturn("Materia SIS S1 M1");
        when(row.getHorarioJson()).thenReturn("[{\"dia\":\"Lunes\",\"inicio\":\"07:00\",\"fin\":\"08:30\"}]");
        when(row.getDocente()).thenReturn("Docente");
        when(row.getAula()).thenReturn("A-01-1");

        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(ofertaMateriaRepository.findHorarioActualRows(7L, 16L, (short) 1)).thenReturn(List.of(row));

        String csv = new String(service.buildHorarioActualCsv(7L), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Horario academico"));
        assertTrue(csv.contains("Universidad,Universidad Catolica Boliviana"));
        assertTrue(csv.contains("Carrera,Ingenieria de Sistemas"));
        assertTrue(csv.contains("Malla,Malla 2024"));
        assertTrue(csv.contains("Semestre oferta,2026-1"));
        assertTrue(csv.contains("Semestre actual,1"));
        assertTrue(csv.contains("Materia,Paralelo,Dia,HoraInicio,HoraFin,Aula,Docente"));
        assertTrue(csv.contains("Materia SIS S1 M1"));
        assertTrue(csv.contains("Docente"));
    }

    @Test
    void buildHorarioActualPdfReturnsContent() {
        HorarioRecomendadoService service = new HorarioRecomendadoService(
            estudianteRepository,
            universidadRepository,
            carreraRepository,
            ofertaMateriaRepository,
            new ObjectMapper()
        );

        Estudiante estudiante = new Estudiante();
        estudiante.setId(7L);
        estudiante.setSemestreActual((short) 1);
        estudiante.setUniversidadId(1L);
        estudiante.setCarreraId(11L);

        Malla malla = new Malla();
        malla.setId(16L);
        malla.setNombre("Malla 2024");
        estudiante.setMalla(malla);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getNombre()).thenReturn("Universidad Catolica Boliviana");

        Carrera carrera = mock(Carrera.class);
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");

        OfertaMateriaRepository.HorarioOfertaRow row = mock(OfertaMateriaRepository.HorarioOfertaRow.class);
        when(row.getSemestre()).thenReturn("2026-1");
        when(row.getParalelo()).thenReturn("A");
        when(row.getMateriaNombre()).thenReturn("Materia SIS S1 M1");
        when(row.getHorarioJson()).thenReturn("[{\"dia\":\"Lunes\",\"inicio\":\"07:00\",\"fin\":\"08:30\"}]");
        when(row.getDocente()).thenReturn("Docente");
        when(row.getAula()).thenReturn("A-01-1");

        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(ofertaMateriaRepository.findHorarioActualRows(7L, 16L, (short) 1)).thenReturn(List.of(row));

        byte[] pdf = service.buildHorarioActualPdf(7L);

        assertNotNull(pdf);
        assertFalse(pdf.length == 0);
    }

    @Test
    void buildHorarioActualImageReturnsPngContent() {
        HorarioRecomendadoService service = new HorarioRecomendadoService(
            estudianteRepository,
            universidadRepository,
            carreraRepository,
            ofertaMateriaRepository,
            new ObjectMapper()
        );

        Estudiante estudiante = new Estudiante();
        estudiante.setId(7L);
        estudiante.setSemestreActual((short) 1);
        estudiante.setUniversidadId(1L);
        estudiante.setCarreraId(11L);

        Malla malla = new Malla();
        malla.setId(16L);
        malla.setNombre("Malla 2024");
        estudiante.setMalla(malla);

        Universidad universidad = mock(Universidad.class);
        when(universidad.getNombre()).thenReturn("Universidad Catolica Boliviana");

        Carrera carrera = mock(Carrera.class);
        when(carrera.getNombre()).thenReturn("Ingenieria de Sistemas");

        OfertaMateriaRepository.HorarioOfertaRow row = mock(OfertaMateriaRepository.HorarioOfertaRow.class);
        when(row.getSemestre()).thenReturn("2026-1");
        when(row.getParalelo()).thenReturn("A");
        when(row.getMateriaNombre()).thenReturn("Materia SIS S1 M1");
        when(row.getHorarioJson()).thenReturn("[{\"dia\":\"Lunes\",\"inicio\":\"07:00\",\"fin\":\"08:30\"}]");
        when(row.getDocente()).thenReturn("Docente");
        when(row.getAula()).thenReturn("A-01-1");

        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(universidadRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(universidad));
        when(carreraRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(carrera));
        when(ofertaMateriaRepository.findHorarioActualRows(7L, 16L, (short) 1)).thenReturn(List.of(row));

        byte[] image = service.buildHorarioActualImage(7L);

        assertNotNull(image);
        assertFalse(image.length == 0);
        assertEquals((byte) 0x89, image[0]);
        assertEquals((byte) 0x50, image[1]);
        assertEquals((byte) 0x4E, image[2]);
        assertEquals((byte) 0x47, image[3]);
    }
}
