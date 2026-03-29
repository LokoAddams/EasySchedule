package com.easyschedule.backend.estudiante.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.easyschedule.backend.estudiante.dto.EstudianteResponse;
import com.easyschedule.backend.estudiante.dto.PerfilUpdateRequest;
import com.easyschedule.backend.estudiante.service.EstudianteService;
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import com.easyschedule.backend.shared.exception.GlobalExceptionHandler;

@WebMvcTest(EstudianteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EstudianteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void findProfileByUsernameReturnsOk() throws Exception {
        when(estudianteService.canAccessProfile("diego", 1L)).thenReturn(true);
        when(estudianteService.findByUsername("diego")).thenReturn(mockResponse("diego"));

        mockMvc.perform(get("/api/estudiantes/perfil/diego").principal(() -> "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("diego"));

        verify(estudianteService).canAccessProfile("diego", 1L);
        verify(estudianteService).findByUsername("diego");
    }

    @Test
    void updateProfileReturnsOkWhenRequestIsValid() throws Exception {
        when(estudianteService.canAccessProfile("diego", 1L)).thenReturn(true);
        when(estudianteService.updateProfile(any(), any())).thenReturn(mockResponse("diego2"));

        String body = """
            {
              "username": "diego2",
              "nombre": "Diego",
              "apellido": "Suarez",
              "email": "diego2@mail.com",
              "carnetIdentidad": "123456",
              "fechaNacimiento": "2001-05-10",
              "carrera": "",
              "universidad": ""
            }
            """;

        mockMvc.perform(put("/api/estudiantes/perfil/diego")
                .principal(() -> "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("diego2"));

        verify(estudianteService).canAccessProfile("diego", 1L);
        verify(estudianteService).updateProfile(any(String.class), any(PerfilUpdateRequest.class));
    }

    @Test
    void updateProfileReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        String invalidBody = """
            {
              "username": "",
              "nombre": "",
              "apellido": "",
              "email": "correo-invalido",
              "carnetIdentidad": "",
              "fechaNacimiento": null,
              "carrera": "",
              "universidad": ""
            }
            """;

        mockMvc.perform(put("/api/estudiantes/perfil/diego")
                .principal(() -> "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest());
    }

    private EstudianteResponse mockResponse(String username) {
        return new EstudianteResponse(
            1L,
            username,
            "Diego",
            "Suarez",
            "diego@mail.com",
            "123456",
            LocalDate.of(2001, 5, 10),
            OffsetDateTime.parse("2026-03-28T12:00:00Z"),
            null,
            null,
            null,
            null,
            false
        );
    }
}
