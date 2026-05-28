package com.easyschedule.backend.estudiante.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import com.easyschedule.backend.estudiante.dto.AvanceGraduacionExport;
import com.easyschedule.backend.estudiante.service.EstudianteMallaExportService;
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

    @MockitoBean
    private EstudianteMallaExportService exportService;


    @Test
    void registerReturnsCreatedWhenRequestIsValid() throws Exception {
        when(estudianteService.register(any())).thenReturn(mockResponse("diego"));

        String requestBody = """
            {
              "username": "diego",
              "email": "diego@mail.com",
              "password": "Abcd1234!"
            }
            """;

        mockMvc.perform(post("/api/estudiantes/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());
    }

    @Test
    void registerReturnsBadRequestWhenPasswordIsWeak() throws Exception {
        String requestBody = """
            {
              "username": "diego",
              "email": "diego@mail.com",
              "password": "abcd1234"
            }
            """;

        mockMvc.perform(post("/api/estudiantes/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

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
              "carnetIdentidad": "1234567-1A LP",
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

    @Test
    void exportarAvanceGraduacionReturnsDownloadablePdfForAuthenticatedUser() throws Exception {
        when(exportService.exportarAvanceGraduacion(1L, "pdf"))
            .thenReturn(new AvanceGraduacionExport("%PDF-test".getBytes(), "application/pdf", "avance_graduacion.pdf"));

        mockMvc.perform(get("/api/estudiantes/me/avance-graduacion/export").principal(() -> "1"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"avance_graduacion.pdf\""));

        verify(exportService).exportarAvanceGraduacion(1L, "pdf");
    }

    @Test
    void exportarAvanceGraduacionReturnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/estudiantes/me/avance-graduacion/export"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfileReturnsBadRequestWhenApellidoContainsNumbers() throws Exception {
        String invalidBody = """
            {
              "username": "diego",
              "nombre": "Diego",
              "apellido": "Suarez1",
              "email": "diego@mail.com",
              "carnetIdentidad": "123456",
              "fechaNacimiento": "2001-05-10",
              "carrera": "",
              "universidad": ""
            }
            """;

        mockMvc.perform(put("/api/estudiantes/perfil/diego")
                .principal(() -> "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Los apellidos solo pueden contener letras, espacios y acentos"));
    }

    @Test
    void updateProfileReturnsBadRequestWhenBirthDateIsUnder16() throws Exception {
        LocalDate under16 = LocalDate.now().minusYears(15);
        String invalidBody = String.format(
            """
            {
              \"username\": \"diego\",
              \"nombre\": \"Diego\",
              \"apellido\": \"Suarez\",
              \"email\": \"diego@mail.com\",
              \"carnetIdentidad\": \"123456\",
              \"fechaNacimiento\": \"%s\",
              \"carrera\": \"\",
              \"universidad\": \"\"
            }
            """,
            under16
        );

        mockMvc.perform(put("/api/estudiantes/perfil/diego")
                .principal(() -> "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("La fecha de nacimiento debe corresponder a una edad entre 16 y 70 años"));
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
            false,
            false
        );
    }
}
