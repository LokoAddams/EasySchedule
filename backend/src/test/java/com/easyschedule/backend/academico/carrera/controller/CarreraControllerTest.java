package com.easyschedule.backend.academico.carrera.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.carrera.dto.CarreraResponse;
import com.easyschedule.backend.academico.carrera.service.CarreraService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CarreraController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarreraControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
