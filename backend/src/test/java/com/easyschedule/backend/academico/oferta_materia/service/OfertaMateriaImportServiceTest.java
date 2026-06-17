package com.easyschedule.backend.academico.oferta_materia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.repository.MateriaRepository;
import com.easyschedule.backend.academico.oferta_materia.dto.Importacion.OfertaImportResultResponse;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class OfertaMateriaImportServiceTest {

    private static final Long MALLA_ID = 1L;
    private static final Long MATERIA_ID = 10L;
    private static final Long MALLA_MATERIA_ID = 100L;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private MallaMateriaRepository mallaMateriaRepository;

    @Mock
    private OfertaMateriaRepository ofertaMateriaRepository;

    private OfertaMateriaImportService service;

    @BeforeEach
    void setUp() {
        service = new OfertaMateriaImportService(
            materiaRepository,
            mallaMateriaRepository,
            ofertaMateriaRepository,
            new ObjectMapper()
        );
    }

    @Test
    void importCsv_whenValidCsvAndOfferDoesNotExist_createsGroupedOffer() {
        Materia materia = buildMateria(MATERIA_ID, "SIS-MAT101", "Calculo I");
        MallaMateria mallaMateria = buildMallaMateria(MALLA_MATERIA_ID, materia);

        when(materiaRepository.findByCodigoIgnoreCase("SIS-MAT101"))
            .thenReturn(Optional.of(materia));

        when(mallaMateriaRepository.findByMallaIdAndMateriaId(MALLA_ID, MATERIA_ID))
            .thenReturn(Optional.of(mallaMateria));

        when(ofertaMateriaRepository.findByMallaMateriaIdAndSemestreAndParalelo(
            MALLA_MATERIA_ID,
            "2026-1",
            "A"
        )).thenReturn(Optional.empty());

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-MAT101,Calculo I,A,2026-1,Lunes,08:00,09:30,Ing. Ana Rojas,A-101
            SIS-MAT101,Calculo I,A,2026-1,Miercoles,08:00,09:30,Ing. Ana Rojas,A-101
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isEmpty();
        assertThat(result.summary().totalRows()).isEqualTo(2);
        assertThat(result.summary().offersCreated()).isEqualTo(1);
        assertThat(result.summary().offersUpdated()).isEqualTo(0);
        assertThat(result.summary().scheduleBlocks()).isEqualTo(2);

        ArgumentCaptor<OfertaMateria> captor = ArgumentCaptor.forClass(OfertaMateria.class);
        verify(ofertaMateriaRepository).save(captor.capture());

        OfertaMateria saved = captor.getValue();

        assertThat(saved.getMallaMateriaId()).isEqualTo(MALLA_MATERIA_ID);
        assertThat(saved.getSemestre()).isEqualTo("2026-1");
        assertThat(saved.getParalelo()).isEqualTo("A");
        assertThat(saved.getDocente()).isEqualTo("Ing. Ana Rojas");
        assertThat(saved.getAula()).isEqualTo("A-101");
        assertThat(saved.getFechaCreacion()).isNotNull();
        assertThat(saved.getFechaActualizacion()).isNotNull();
        assertThat(saved.getHorarioJson()).contains("\"dia\":\"Lunes\"");
        assertThat(saved.getHorarioJson()).contains("\"dia\":\"Miercoles\"");
        assertThat(saved.getHorarioJson()).contains("\"horaInicio\":\"08:00\"");
        assertThat(saved.getHorarioJson()).contains("\"horaFin\":\"09:30\"");
    }

    @Test
    void importCsv_whenOfferAlreadyExists_updatesOfferInsteadOfCreatingDuplicate() {
        Materia materia = buildMateria(MATERIA_ID, "SIS-BD101", "Base de Datos I");
        MallaMateria mallaMateria = buildMallaMateria(MALLA_MATERIA_ID, materia);
        OfertaMateria existingOffer = buildExistingOffer();

        when(materiaRepository.findByCodigoIgnoreCase("SIS-BD101"))
            .thenReturn(Optional.of(materia));

        when(mallaMateriaRepository.findByMallaIdAndMateriaId(MALLA_ID, MATERIA_ID))
            .thenReturn(Optional.of(mallaMateria));

        when(ofertaMateriaRepository.findByMallaMateriaIdAndSemestreAndParalelo(
            MALLA_MATERIA_ID,
            "2026-1",
            "B"
        )).thenReturn(Optional.of(existingOffer));

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-BD101,Base de Datos I,B,2026-1,Viernes,10:00,12:00,Ing. Valeria Alvarez,LAB-306
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isEmpty();
        assertThat(result.summary().offersCreated()).isEqualTo(0);
        assertThat(result.summary().offersUpdated()).isEqualTo(1);

        verify(ofertaMateriaRepository).save(existingOffer);

        assertThat(existingOffer.getDocente()).isEqualTo("Ing. Valeria Alvarez");
        assertThat(existingOffer.getAula()).isEqualTo("LAB-306");
        assertThat(existingOffer.getHorarioJson()).contains("\"dia\":\"Viernes\"");
        assertThat(existingOffer.getHorarioJson()).contains("\"horaInicio\":\"10:00\"");
        assertThat(existingOffer.getHorarioJson()).contains("\"horaFin\":\"12:00\"");
        assertThat(existingOffer.getFechaActualizacion()).isNotNull();
    }

    @Test
    void importCsv_whenFileIsEmpty_returnsCriticalErrorAndDoesNotSave() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "ofertas.csv",
            "text/csv",
            new byte[0]
        );

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).field()).isEqualTo("archivo");
        assertThat(result.errors().get(0).critical()).isTrue();
        assertThat(result.summary().offersCreated()).isZero();
        assertThat(result.summary().offersUpdated()).isZero();

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenFileExtensionIsNotCsv_returnsCriticalErrorAndDoesNotSave() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "ofertas.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "contenido".getBytes(StandardCharsets.UTF_8)
        );

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).field()).isEqualTo("archivo");
        assertThat(result.errors().get(0).reason()).contains("CSV");

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenRequiredColumnIsMissing_returnsCriticalErrorAndDoesNotSave() {
        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-MAT101,Calculo I,2026-1,Lunes,08:00,09:30,Ing. Ana Rojas,A-101
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors())
            .anyMatch(error -> error.field().equals("paralelo") && error.critical());

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenMateriaDoesNotExist_returnsCriticalErrorAndDoesNotSave() {
        when(materiaRepository.findByCodigoIgnoreCase("SIS-XXX999"))
            .thenReturn(Optional.empty());

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-XXX999,Materia Inexistente,A,2026-1,Lunes,08:00,09:30,Docente Demo,A-101
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors())
            .anyMatch(error ->
                error.rowNumber() == 2
                    && error.field().equals("codigo_materia")
                    && error.critical()
            );

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenMateriaDoesNotBelongToMalla_returnsCriticalErrorAndDoesNotSave() {
        Materia materia = buildMateria(MATERIA_ID, "SIS-MAT101", "Calculo I");

        when(materiaRepository.findByCodigoIgnoreCase("SIS-MAT101"))
            .thenReturn(Optional.of(materia));

        when(mallaMateriaRepository.findByMallaIdAndMateriaId(MALLA_ID, MATERIA_ID))
            .thenReturn(Optional.empty());

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-MAT101,Calculo I,A,2026-1,Lunes,08:00,09:30,Ing. Ana Rojas,A-101
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors())
            .anyMatch(error ->
                error.rowNumber() == 2
                    && error.field().equals("codigo_materia")
                    && error.reason().contains("no pertenece")
            );

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenHourRangeIsInvalid_returnsCriticalErrorAndDoesNotSave() {
        Materia materia = buildMateria(MATERIA_ID, "SIS-INF102", "Programacion Orientada a Objetos");
        MallaMateria mallaMateria = buildMallaMateria(MALLA_MATERIA_ID, materia);

        when(materiaRepository.findByCodigoIgnoreCase("SIS-INF102"))
            .thenReturn(Optional.of(materia));

        when(mallaMateriaRepository.findByMallaIdAndMateriaId(MALLA_ID, MATERIA_ID))
            .thenReturn(Optional.of(mallaMateria));

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-INF102,Programacion Orientada a Objetos,A,2026-1,Viernes,15:30,14:00,Ing. Daniela Vargas,LAB-202
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors())
            .anyMatch(error -> error.field().equals("horario") && error.critical());

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenDayIsInvalid_returnsCriticalErrorAndDoesNotSave() {
        Materia materia = buildMateria(MATERIA_ID, "SIS-INF102", "Programacion Orientada a Objetos");
        MallaMateria mallaMateria = buildMallaMateria(MALLA_MATERIA_ID, materia);

        when(materiaRepository.findByCodigoIgnoreCase("SIS-INF102"))
            .thenReturn(Optional.of(materia));

        when(mallaMateriaRepository.findByMallaIdAndMateriaId(MALLA_ID, MATERIA_ID))
            .thenReturn(Optional.of(mallaMateria));

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-INF102,Programacion Orientada a Objetos,A,2026-1,Feriado,10:00,11:30,Ing. Daniela Vargas,LAB-202
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors())
            .anyMatch(error -> error.field().equals("dia") && error.critical());

        verify(ofertaMateriaRepository, never()).save(any(OfertaMateria.class));
    }

    @Test
    void importCsv_whenDocenteAndAulaAreEmpty_returnsWarningsButStillCreatesOffer() {
        Materia materia = buildMateria(MATERIA_ID, "SIS-MAT102", "Algebra Lineal");
        MallaMateria mallaMateria = buildMallaMateria(MALLA_MATERIA_ID, materia);

        when(materiaRepository.findByCodigoIgnoreCase("SIS-MAT102"))
            .thenReturn(Optional.of(materia));

        when(mallaMateriaRepository.findByMallaIdAndMateriaId(MALLA_ID, MATERIA_ID))
            .thenReturn(Optional.of(mallaMateria));

        when(ofertaMateriaRepository.findByMallaMateriaIdAndSemestreAndParalelo(
            MALLA_MATERIA_ID,
            "2026-1",
            "A"
        )).thenReturn(Optional.empty());

        MockMultipartFile file = csvFile("""
            codigo_materia,nombre_materia,paralelo,semestre_academico,dia,hora_inicio,hora_fin,docente,aula
            SIS-MAT102,Algebra Lineal,A,2026-1,Lunes,14:00,15:30,,
            """);

        OfertaImportResultResponse result = service.importCsv(MALLA_ID, file);

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).hasSize(2);
        assertThat(result.summary().offersCreated()).isEqualTo(1);

        verify(ofertaMateriaRepository).save(any(OfertaMateria.class));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "ofertas.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Materia buildMateria(Long id, String codigo, String nombre) {
        Materia materia = new Materia();

        materia.setId(id);
        materia.setCodigo(codigo);
        materia.setNombre(nombre);
        materia.setCreditos((short) 5);
        materia.setActive(true);

        return materia;
    }

    private MallaMateria buildMallaMateria(Long id, Materia materia) {
        MallaMateria mallaMateria = new MallaMateria();

        mallaMateria.setId(id);
        mallaMateria.setMateria(materia);
        mallaMateria.setSemestreSugerido((short) 1);

        return mallaMateria;
    }

    private OfertaMateria buildExistingOffer() {
        OfertaMateria ofertaMateria = new OfertaMateria();

        ofertaMateria.setId(999L);
        ofertaMateria.setMallaMateriaId(MALLA_MATERIA_ID);
        ofertaMateria.setSemestre("2026-1");
        ofertaMateria.setParalelo("B");
        ofertaMateria.setHorarioJson("[{\"dia\":\"Lunes\",\"inicio\":\"08:00\",\"fin\":\"09:30\"}]");
        ofertaMateria.setDocente("Docente anterior");
        ofertaMateria.setAula("Aula anterior");
        ofertaMateria.setFechaCreacion(OffsetDateTime.now().minusDays(1));
        ofertaMateria.setFechaActualizacion(OffsetDateTime.now().minusDays(1));

        return ofertaMateria;
    }
}