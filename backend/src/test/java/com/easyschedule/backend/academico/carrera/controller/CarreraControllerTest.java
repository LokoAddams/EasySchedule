package com.easyschedule.backend.academico.carrera.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.carrera.dto.CarreraRequest;
import com.easyschedule.backend.academico.carrera.dto.CarreraResponse;
import com.easyschedule.backend.academico.carrera.service.CarreraService;
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

@WebMvcTest(CarreraController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarreraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CarreraService carreraService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void findByUniversidadReturnsCarrerasActivas() throws Exception {
        when(carreraService.findActiveByUniversidad(1L)).thenReturn(List.of(
            new CarreraResponse(10L, 1L, "Ingenieria de Sistemas", "SIS"),
            new CarreraResponse(11L, 1L, "Ingenieria Civil", "CIV")
        ));

        mockMvc.perform(get("/api/academico/carreras").param("universidadId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Ingenieria de Sistemas"))
            .andExpect(jsonPath("$[1].codigo").value("CIV"));

        verify(carreraService).findActiveByUniversidad(1L);
    }

    @Test
    void createCarreraReturnsCreated() throws Exception {
        CarreraRequest request = new CarreraRequest(1L, "Nueva Carrera", "NC", true);
        CarreraResponse response = new CarreraResponse(20L, 1L, "Nueva Carrera", "NC");

        when(carreraService.createCarrera(any(CarreraRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/academico/carreras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Nueva Carrera"))
            .andExpect(jsonPath("$.codigo").value("NC"));

        verify(carreraService).createCarrera(any(CarreraRequest.class));
    }

    @Test
    void createCarreraReturnsBadRequestWhenNombreMissing() throws Exception {
        String invalidJson = "{\"universidadId\": 1, \"codigo\": \"NC\"}";

        mockMvc.perform(post("/api/academico/carreras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createCarreraReturnsBadRequestWhenUniversidadIdMissing() throws Exception {
        String invalidJson = "{\"nombre\": \"Nueva Carrera\", \"codigo\": \"NC\"}";

        mockMvc.perform(post("/api/academico/carreras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
}
