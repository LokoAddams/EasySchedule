package com.easyschedule.backend.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.service.AuthService;
import com.easyschedule.backend.shared.exception.GlobalExceptionHandler;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerUserReturnsCreatedWhenRequestIsValid() throws Exception {
        String requestBody = """
                {
                  "username": "newuser",
                  "email": "newuser@mail.com",
                  "password": "123456",
                  "role": ["user"]
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        verify(authService).registerUser(any(SignupRequest.class));
    }

    @Test
    void registerUserReturnsConflictWhenUserAlreadyExists() throws Exception {
        doThrow(new UserAlreadyExistsException("Error: El nombre de usuario ya está en uso"))
                .when(authService)
                .registerUser(any(SignupRequest.class));

        String requestBody = """
                {
                  "username": "duplicate",
                  "email": "duplicate@mail.com",
                  "password": "123456"
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Error: El nombre de usuario ya está en uso"));
    }

    @Test
    void registerUserReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        String requestBody = """
                {
                  "username": "",
                  "email": "correo-invalido",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(SignupRequest.class));
    }
}
