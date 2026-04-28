package com.easyschedule.backend.academico.horario.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.academico.horario.dto.HorarioActualResponse;
import com.easyschedule.backend.academico.horario.dto.HorarioClaseResponse;
import com.easyschedule.backend.academico.horario.service.HorarioRecomendadoService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HorarioRecomendadoController.class)
@AutoConfigureMockMvc(addFilters = false)
class HorarioRecomendadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HorarioRecomendadoService horarioRecomendadoService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void getHorarioActualReturnsDataForAuthenticatedUser() throws Exception {
        when(horarioRecomendadoService.getHorarioActualByUserId(7L)).thenReturn(
            new HorarioActualResponse(
                "Universidad Catolica Boliviana",
                "Ingenieria de Sistemas",
                "Malla 2024",
                "2026-1",
                (short) 1,
                List.of(new HorarioClaseResponse("Materia SIS S1 M1", "A", "Lunes", "07:00", "08:30", "Docente", "A-01-1", 3))
            )
        );

        mockMvc.perform(get("/api/academico/horario/actual").principal(() -> "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.carrera").value("Ingenieria de Sistemas"))
            .andExpect(jsonPath("$.clases[0].dia").value("Lunes"));

        verify(horarioRecomendadoService).getHorarioActualByUserId(7L);
    }

    @Test
    void getHorarioActualReturnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/academico/horario/actual"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void exportHorarioActualReturnsCsvWhenAuthorized() throws Exception {
        byte[] payload = "Materia,Paralelo".getBytes();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        when(horarioRecomendadoService.hasHorarioActual(7L)).thenReturn(true);
        when(horarioRecomendadoService.buildHorarioActualCsv(7L)).thenReturn(payload);

        mockMvc.perform(get("/api/academico/horario/actual/7/export")
                .param("formato", "csv")
                .principal(() -> "7"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("text/csv")))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("horario_7_" + today + ".csv")));

        verify(horarioRecomendadoService).hasHorarioActual(7L);
        verify(horarioRecomendadoService).buildHorarioActualCsv(7L);
    }

    @Test
    void exportHorarioActualReturnsPdfWhenAuthorized() throws Exception {
        byte[] payload = new byte[] { 1, 2, 3 };
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        when(horarioRecomendadoService.hasHorarioActual(7L)).thenReturn(true);
        when(horarioRecomendadoService.buildHorarioActualPdf(7L)).thenReturn(payload);

        mockMvc.perform(get("/api/academico/horario/actual/7/export")
                .param("formato", "pdf")
                .principal(() -> "7"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("horario_7_" + today + ".pdf")));

        verify(horarioRecomendadoService).hasHorarioActual(7L);
        verify(horarioRecomendadoService).buildHorarioActualPdf(7L);
    }

    @Test
    void exportHorarioActualReturnsImageWhenAuthorized() throws Exception {
        byte[] payload = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        when(horarioRecomendadoService.hasHorarioActual(7L)).thenReturn(true);
        when(horarioRecomendadoService.buildHorarioActualImage(7L)).thenReturn(payload);

        mockMvc.perform(get("/api/academico/horario/actual/7/export")
                .param("formato", "imagen")
                .principal(() -> "7"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("horario_7_" + today + ".png")));

        verify(horarioRecomendadoService).hasHorarioActual(7L);
        verify(horarioRecomendadoService).buildHorarioActualImage(7L);
    }

    @Test
    void exportHorarioActualReturnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/academico/horario/actual/7/export"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void exportHorarioActualReturnsForbiddenWhenDifferentUser() throws Exception {
        mockMvc.perform(get("/api/academico/horario/actual/8/export")
                .param("formato", "csv")
                .principal(() -> "7"))
            .andExpect(status().isForbidden());
    }

    @Test
    void exportHorarioActualReturnsNotFoundWhenNoHorario() throws Exception {
        when(horarioRecomendadoService.hasHorarioActual(7L)).thenReturn(false);

        mockMvc.perform(get("/api/academico/horario/actual/7/export")
                .param("formato", "csv")
                .principal(() -> "7"))
            .andExpect(status().isNotFound());

        verify(horarioRecomendadoService).hasHorarioActual(7L);
    }

    @Test
    void exportHorarioActualReturnsBadRequestForUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/academico/horario/actual/7/export")
                .param("formato", "xlsx")
                .principal(() -> "7"))
            .andExpect(status().isBadRequest());
    }
}
