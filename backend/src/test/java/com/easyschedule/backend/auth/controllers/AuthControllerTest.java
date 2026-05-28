package com.easyschedule.backend.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import com.easyschedule.backend.shared.exception.GlobalExceptionHandler;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;

import static org.mockito.Mockito.when;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.easyschedule.backend.auth.dto.request.GoogleLoginRequest;
import org.springframework.http.HttpStatus;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @Test
    void registerUserReturnsCreatedWhenRequestIsValid() throws Exception {
        String requestBody = """
                {
                  "username": "newuser",
                  "email": "newuser@mail.com",
                  "password": "123456"
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

    @Test
    void changePasswordReturnsOkWhenRequestIsValid() throws Exception {
        String requestBody = """
                {
                  "currentPassword": "actual1234",
                  "newPassword": "nuevaPassword123",
                  "confirmNewPassword": "nuevaPassword123"
                }
                """;

        mockMvc.perform(post("/api/change-password")
                        .principal(() -> "19")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void googleLoginDelegatesToAuthService() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok(Map.of(
                "token", "internal-token",
                "username", "testuser",
                "expiresInSeconds", 3600L,
                "message", "Login con Google exitoso"
        ));

        doReturn(response)
                .when(authService)
                .loginWithGoogle(any(GoogleLoginRequest.class));

        String requestBody = """
                {
                "credential": "valid-google-token"
                }
                """;

        mockMvc.perform(post("/api/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("internal-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.message").value("Login con Google exitoso"));

        verify(authService).loginWithGoogle(any(GoogleLoginRequest.class));
    }

    @Test
    void googleLoginReturnsUnauthorizedWhenCredentialIsMissing() throws Exception {
        ResponseEntity<?> response = ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Login con Google invalido");

        doReturn(response)
                .when(authService)
                .loginWithGoogle(any(GoogleLoginRequest.class));

        String requestBody = """
                {
                }
                """;

        mockMvc.perform(post("/api/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        verify(authService).loginWithGoogle(any(GoogleLoginRequest.class));
    }
}
