package com.easyschedule.backend.academico.malla.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.service.MallaService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MallaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MallaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MallaService mallaService;

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
}
