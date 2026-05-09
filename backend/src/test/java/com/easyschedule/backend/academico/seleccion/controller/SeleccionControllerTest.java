package com.easyschedule.backend.academico.seleccion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.seleccion.service.SeleccionService;
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
    void listSelections_ReturnsList() throws Exception {
        SeleccionResponse res = new SeleccionResponse();
        res.setId(1L);
        res.setMateriaNombre("Calculo I");

        when(seleccionService.listByUserId(1L)).thenReturn(List.of(res));

        mockMvc.perform(get("/api/academico/seleccion-temporal").principal(() -> "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materiaNombre").value("Calculo I"));
    }

    @Test
    void addSelection_ReturnsCreated() throws Exception {
        SeleccionRequest request = new SeleccionRequest();
        request.setOfertaMateriaId(100L);

        SeleccionResponse res = new SeleccionResponse();
        res.setId(1L);
        res.setOfertaMateriaId(100L);

        when(seleccionService.addSelection(eq(1L), any(SeleccionRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/academico/seleccion-temporal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(() -> "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ofertaMateriaId").value(100));
    }

    @Test
    void removeSelection_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/academico/seleccion-temporal/100").principal(() -> "1"))
                .andExpect(status().isNoContent());

        verify(seleccionService).removeSelection(1L, 100L);
    }

    @Test
    void clearSelections_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/academico/seleccion-temporal/clear").principal(() -> "1"))
                .andExpect(status().isNoContent());

        verify(seleccionService).clearSelections(1L);
    }
}
