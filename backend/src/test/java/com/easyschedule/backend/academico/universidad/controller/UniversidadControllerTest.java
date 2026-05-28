package com.easyschedule.backend.academico.universidad.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.universidad.dto.UniversidadRequest;
import com.easyschedule.backend.academico.universidad.dto.UniversidadResponse;
import com.easyschedule.backend.academico.universidad.service.UniversidadService;
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

@WebMvcTest(UniversidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class UniversidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UniversidadService universidadService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void findAllReturnsUniversidadesActivas() throws Exception {
        when(universidadService.findAllActive()).thenReturn(List.of(
            new UniversidadResponse(1L, "Universidad Catolica Boliviana", "UCB"),
            new UniversidadResponse(2L, "Universidad Mayor de San Simon", "UMSS")
        ));

        mockMvc.perform(get("/api/academico/universidades"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].codigo").value("UCB"))
            .andExpect(jsonPath("$[1].codigo").value("UMSS"));

        verify(universidadService).findAllActive();
    }

    @Test
    void createUniversidadReturnsCreated() throws Exception {
        UniversidadRequest request = new UniversidadRequest("Nueva Universidad", "NU", true);
        UniversidadResponse response = new UniversidadResponse(10L, "Nueva Universidad", "NU");

        when(universidadService.createUniversidad(any(UniversidadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/academico/universidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Nueva Universidad"))
            .andExpect(jsonPath("$.codigo").value("NU"));

        verify(universidadService).createUniversidad(any(UniversidadRequest.class));
    }

    @Test
    void createUniversidadReturnsBadRequestWhenNombreMissing() throws Exception {
        String invalidJson = "{\"codigo\": \"NU\"}";

        mockMvc.perform(post("/api/academico/universidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUniversidadReturnsBadRequestWhenCodigoMissing() throws Exception {
        String invalidJson = "{\"nombre\": \"Nueva Universidad\"}";

        mockMvc.perform(post("/api/academico/universidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
}
