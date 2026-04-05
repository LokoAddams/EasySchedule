package com.easyschedule.backend.academico.seleccion.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.seleccion.service.SeleccionService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SeleccionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeleccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SeleccionService seleccionService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void getSeleccionReturnsDataForAuthenticatedUser() throws Exception {
        when(seleccionService.getSeleccionByUserId(7L)).thenReturn(
            new SeleccionResponse(1L, "Universidad Catolica Boliviana", 11L, "Ingenieria de Sistemas", 101L, "Malla 2017")
        );

        mockMvc.perform(get("/api/academico/seleccion").principal(() -> "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.universidad").value("Universidad Catolica Boliviana"))
            .andExpect(jsonPath("$.mallaId").value(101));

        verify(seleccionService).getSeleccionByUserId(7L);
    }

    @Test
    void getSeleccionReturnsUnauthorizedWhenPrincipalIsMissing() throws Exception {
        mockMvc.perform(get("/api/academico/seleccion"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void saveSeleccionReturnsUnauthorizedWhenPrincipalIsInvalid() throws Exception {
        SeleccionRequest request = new SeleccionRequest(1L, 2L, 3L);

        mockMvc.perform(put("/api/academico/seleccion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(() -> "not-a-number"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void saveSeleccionDelegatesToServiceForAuthenticatedUser() throws Exception {
        SeleccionRequest request = new SeleccionRequest(1L, 2L, 3L);
        when(seleccionService.saveSeleccionByUserId(9L, request)).thenReturn(
            new SeleccionResponse(1L, "UCB", 2L, "Sistemas", 3L, "Malla 2017")
        );

        mockMvc.perform(put("/api/academico/seleccion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(() -> "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.carrera").value("Sistemas"));

        verify(seleccionService).saveSeleccionByUserId(9L, request);
    }
}
