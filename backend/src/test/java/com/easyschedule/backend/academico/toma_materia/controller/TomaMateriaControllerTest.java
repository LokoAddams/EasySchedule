package com.easyschedule.backend.academico.toma_materia.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaRequest;
import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaResponse;
import com.easyschedule.backend.academico.toma_materia.service.TomaMateriaService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TomaMateriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class TomaMateriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TomaMateriaService tomaMateriaService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void listByCurrentUserReturnsRows() throws Exception {
        when(tomaMateriaService.listByUserId(9L)).thenReturn(List.of(
            new TomaMateriaResponse(1L, 9L, 77L, "inscrita", OffsetDateTime.now(), OffsetDateTime.now())
        ));

        mockMvc.perform(get("/api/academico/toma-materias").principal(() -> "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ofertaId").value(77))
            .andExpect(jsonPath("$[0].estado").value("inscrita"));

        verify(tomaMateriaService).listByUserId(9L);
    }

    @Test
    void listByCurrentUserReturnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/academico/toma-materias"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void saveByCurrentUserDelegatesToService() throws Exception {
        TomaMateriaRequest request = new TomaMateriaRequest(List.of(77L, 88L));
        when(tomaMateriaService.saveByUserId(9L, request)).thenReturn(List.of(
            new TomaMateriaResponse(1L, 9L, 77L, "inscrita", OffsetDateTime.now(), OffsetDateTime.now()),
            new TomaMateriaResponse(2L, 9L, 88L, "inscrita", OffsetDateTime.now(), OffsetDateTime.now())
        ));

        mockMvc.perform(post("/api/academico/toma-materias")
                .principal(() -> "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ofertaId").value(77))
            .andExpect(jsonPath("$[1].ofertaId").value(88));

        verify(tomaMateriaService).saveByUserId(9L, request);
    }

    @Test
    void deleteByOfertaDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/academico/toma-materias/oferta/77").principal(() -> "9"))
            .andExpect(status().isOk());

        verify(tomaMateriaService).deleteByUserIdAndOfertaId(9L, 77L);
    }
}
