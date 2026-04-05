package com.easyschedule.backend.academico.universidad.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.universidad.dto.UniversidadResponse;
import com.easyschedule.backend.academico.universidad.service.UniversidadService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UniversidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class UniversidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
