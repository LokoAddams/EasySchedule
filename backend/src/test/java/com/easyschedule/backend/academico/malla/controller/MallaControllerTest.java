package com.easyschedule.backend.academico.malla.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.malla.dto.MallaImportRequest;
import com.easyschedule.backend.academico.malla.dto.MallaImportResponse;
import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.dto.MateriaImportRequest;
import com.easyschedule.backend.academico.malla.service.MallaFileParserService;
import com.easyschedule.backend.academico.malla.service.MallaImportService;
import com.easyschedule.backend.academico.malla.service.MallaService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MallaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MallaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MallaService mallaService;

    @MockitoBean
    private MallaFileParserService fileParserService;

    @MockitoBean
    private MallaImportService mallaImportService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void findByCarreraReturnsMallasActivas() throws Exception {
        when(mallaService.findActiveByCarrera(10L)).thenReturn(List.of(
            new MallaResponse(100L, 10L, "Malla 2017", "2017", true),
            new MallaResponse(101L, 10L, "Malla 2024", "2024", true)
        ));

        mockMvc.perform(get("/api/academico/mallas").param("carreraId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].version").value("2017"))
            .andExpect(jsonPath("$[1].nombre").value("Malla 2024"));

        verify(mallaService).findActiveByCarrera(10L);
    }

    @Test
    void findMateriasByMallaReturnsEstadoForAuthenticatedUser() throws Exception {
        when(mallaService.findMateriasByMalla(16L, 7L)).thenReturn(List.of(
            new MallaMateriaResponse(501L, 33L, "SIS101", "Algoritmos", (short) 4, (short) 1, "aprobada", List.of())
        ));

        mockMvc.perform(get("/api/academico/mallas/16/materias").principal(() -> "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].codigoMateria").value("SIS101"))
            .andExpect(jsonPath("$[0].estado").value("aprobada"));

        verify(mallaService).findMateriasByMalla(16L, 7L);
    }

    @Test
    void findMateriasByMallaReturnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/academico/mallas/16/materias"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void crearMallaReturnsCreated() throws Exception {
        MateriaImportRequest materia = new MateriaImportRequest("SIS101", "Algoritmos", 1, 4, List.of());
        MallaImportRequest request = new MallaImportRequest("Malla Test", "2026", 10L, List.of(materia));
        MallaImportResponse response = new MallaImportResponse(100L, "Malla Test", 1, 0, "Malla creada exitosamente");

        when(mallaImportService.importarMalla(any(MallaImportRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/academico/mallas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.mallaId").value(100))
            .andExpect(jsonPath("$.materiasImportadas").value(1));

        verify(mallaImportService).importarMalla(any(MallaImportRequest.class));
    }

    @Test
    void crearMallaReturnsBadRequestWhenInvalid() throws Exception {
        MallaImportRequest request = new MallaImportRequest("", "2026", 10L, List.of());

        when(mallaImportService.importarMalla(any(MallaImportRequest.class)))
            .thenThrow(new IllegalArgumentException("El nombre de la malla es requerido"));

        mockMvc.perform(post("/api/academico/mallas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
