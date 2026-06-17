package com.easyschedule.backend.academico.malla.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import com.easyschedule.backend.academico.malla.dto.MallaEditRequest;
import com.easyschedule.backend.academico.malla.dto.MallaEditResponse;
import com.easyschedule.backend.academico.malla.dto.MateriaImportRequest;
import com.easyschedule.backend.academico.malla.model.Malla;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.malla.repository.MallaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.model.Prerequisito;
import com.easyschedule.backend.academico.materia.repository.MateriaRepository;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MallaImportServiceTest {

    @Mock
    private MallaRepository mallaRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private MallaMateriaRepository mallaMateriaRepository;

    @Mock
    private PrerequisitoRepository prerequisitoRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @Test
    void getMallaEditableReturnsPrerequisiteCodes() {
        MallaImportService service = createService();
        Malla malla = new Malla(8L, 3L, "Malla 2026", "2026");
        Materia algoritmos = new Materia(1L, "SIS101", "Algoritmos", (short) 4);
        Materia programacion = new Materia(2L, "SIS102", "Programacion", (short) 5);
        MallaMateria prereq = new MallaMateria(11L, malla, algoritmos);
        prereq.setSemestreSugerido((short) 1);
        MallaMateria subject = new MallaMateria(12L, malla, programacion);
        subject.setSemestreSugerido((short) 2);
        Prerequisito prerequisito = new Prerequisito(99L, subject, prereq);

        when(mallaRepository.findByIdAndActiveTrue(8L)).thenReturn(Optional.of(malla));
        when(mallaMateriaRepository.findByMallaIdOrderBySemestreSugeridoAsc(8L)).thenReturn(List.of(prereq, subject));
        when(prerequisitoRepository.findByMallaMateria_Id(11L)).thenReturn(List.of());
        when(prerequisitoRepository.findByMallaMateria_Id(12L)).thenReturn(List.of(prerequisito));

        MallaEditResponse response = service.getMallaEditable(8L);

        assertEquals(8L, response.mallaId());
        assertEquals("SIS102", response.materias().get(1).codigo());
        assertEquals(List.of("SIS101"), response.materias().get(1).prerequisitos());
    }

    @Test
    void actualizarMallaRejectsUnknownPrerequisiteCodes() {
        MallaImportService service = createService();
        Malla malla = new Malla(8L, 3L, "Malla 2026", "2026");
        MallaEditRequest request = new MallaEditRequest(List.of(
            new MateriaImportRequest("SIS102", "Programacion", 2, 5, List.of("SIS999"))
        ));

        when(mallaRepository.findByIdAndActiveTrue(8L)).thenReturn(Optional.of(malla));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.actualizarMalla(8L, request)
        );

        assertEquals("El prerequisito 'SIS999' no existe en la malla", exception.getMessage());
    }

    private MallaImportService createService() {
        return new MallaImportService(
            mallaRepository,
            materiaRepository,
            mallaMateriaRepository,
            prerequisitoRepository,
            carreraRepository
        );
    }
}
