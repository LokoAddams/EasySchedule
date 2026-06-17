package com.easyschedule.backend.academico.oferta_materia.service;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.academico.malla.repository.MallaMateriaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.oferta_materia.dto.HorarioDto;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaEdicionResponse;
import com.easyschedule.backend.academico.oferta_materia.dto.OfertaMateriaUpdateRequest;
import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfertaMateriaServiceTest {

    @Mock
    private OfertaMateriaRepository ofertaMateriaRepository;

    @Mock
    private MallaMateriaRepository mallaMateriaRepository;

    @Mock
    private PrerequisitoRepository prerequisitoRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private OfertaMateriaService ofertaMateriaService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        ofertaMateriaService = new OfertaMateriaService(
                ofertaMateriaRepository,
                mallaMateriaRepository,
                prerequisitoRepository,
                objectMapper
        );
    }

    @Test
    void obtenerParaEdicion_DebeRetornarOfertaMapeada() throws Exception {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setId(1L);
        oferta.setMallaMateriaId(100L);
        oferta.setParalelo("A");
        oferta.setSemestre("1");
        oferta.setDocente("Juan Perez");
        oferta.setAula("Aula 1");
        oferta.setHorarioJson("[{\"dia\":\"Lunes\",\"inicio\":\"08:00\",\"fin\":\"09:30\"}]");

        MallaMateria mallaMateria = new MallaMateria();
        mallaMateria.setId(100L);

        Materia materia = new Materia();
        materia.setId(200L);
        materia.setCodigo("MAT-101");
        materia.setNombre("Matematicas");
        
        mallaMateria.setMateria(materia);

        when(ofertaMateriaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(mallaMateriaRepository.findById(100L)).thenReturn(Optional.of(mallaMateria));

        OfertaMateriaEdicionResponse response = ofertaMateriaService.obtenerParaEdicion(1L);

        assertThat(response).isNotNull();
        assertThat(response.codigoMateria()).isEqualTo("MAT-101");
        assertThat(response.nombreMateria()).isEqualTo("Matematicas");
        assertThat(response.docente()).isEqualTo("Juan Perez");
        assertThat(response.horarios()).hasSize(1);
        assertThat(response.horarios().get(0).horaInicio()).isEqualTo("08:00");
    }

    @Test
    void validarActualizacion_SinCruces_PasaSinExcepcion() {
        OfertaMateriaUpdateRequest request = new OfertaMateriaUpdateRequest(
                "MAT-101", "Matematicas", "A", "1", "Juan Perez", "Aula 1",
                List.of(new HorarioDto("Lunes", "08:00", "09:30"))
        );

        when(ofertaMateriaRepository.findByAulaAndSemestreAndIdNot("Aula 1", "1", 1L)).thenReturn(List.of());

        ofertaMateriaService.validarActualizacion(1L, request);
    }

    @Test
    void validarActualizacion_DiferenteAula_PasaSinExcepcion() {
        OfertaMateriaUpdateRequest request = new OfertaMateriaUpdateRequest(
                "MAT-101", "Matematicas", "A", "1", "Juan Perez", "Aula 1",
                List.of(new HorarioDto("Lunes", "08:00", "09:30"))
        );

        when(ofertaMateriaRepository.findByAulaAndSemestreAndIdNot("Aula 1", "1", 1L)).thenReturn(List.of());

        ofertaMateriaService.validarActualizacion(1L, request);
    }

    @Test
    void validarActualizacion_ConCruceHorarioMismaAula_LanzaExcepcion() {
        OfertaMateria otraOferta = new OfertaMateria();
        otraOferta.setId(2L);
        otraOferta.setSemestre("1");
        otraOferta.setAula("Aula 1");
        otraOferta.setHorarioJson("[{\"dia\":\"Lunes\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}]");

        OfertaMateriaUpdateRequest request = new OfertaMateriaUpdateRequest(
                "MAT-101", "Matematicas", "A", "1", "Juan Perez", "Aula 1",
                List.of(new HorarioDto("Lunes", "09:00", "11:00"))
        );

        when(ofertaMateriaRepository.findByAulaAndSemestreAndIdNot("Aula 1", "1", 1L)).thenReturn(List.of(otraOferta));

        assertThatThrownBy(() -> ofertaMateriaService.validarActualizacion(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choque de horario detectado en el aula Aula 1");
    }

    @Test
    void actualizarOferta_DatosValidos_GuardaCorrectamente() throws Exception {
        OfertaMateria oferta = new OfertaMateria();
        oferta.setId(1L);
        oferta.setMallaMateriaId(100L);
        oferta.setSemestre("1");

        Materia materia = new Materia();
        materia.setId(200L);
        materia.setCodigo("MAT-101");

        MallaMateria mallaMateria = new MallaMateria();
        mallaMateria.setId(100L);
        mallaMateria.setMateria(materia);

        OfertaMateriaUpdateRequest request = new OfertaMateriaUpdateRequest(
                "MAT-101", "Matematicas", "B", "1", "Maria Lopez", "Aula 5",
                List.of(new HorarioDto("Martes", "10:00", "12:00"))
        );

        when(ofertaMateriaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(ofertaMateriaRepository.findByAulaAndSemestreAndIdNot("Aula 5", "1", 1L)).thenReturn(List.of());
        when(mallaMateriaRepository.findById(100L)).thenReturn(Optional.of(mallaMateria));

        ofertaMateriaService.actualizarOferta(1L, request);

        verify(ofertaMateriaRepository).save(oferta);
        assertThat(oferta.getParalelo()).isEqualTo("B");
        assertThat(oferta.getDocente()).isEqualTo("Maria Lopez");
        assertThat(oferta.getAula()).isEqualTo("Aula 5");
        assertThat(oferta.getHorarioJson()).contains("horaInicio");
    }
}
