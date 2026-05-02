package com.easyschedule.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.easyschedule.backend.auth.dto.request.ChangePasswordRequest;
import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private SessionTokenService sessionTokenService;

    @InjectMocks
    private AuthService authService;

    private SignupRequest request;

    @BeforeEach
    void setUp() {
        request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@mail.com");
        request.setPassword("123456");
    }

    @Test
    void registerUserSavesUserWithEncodedPassword() {
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("testuser@mail.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("encoded-password");

        authService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("testuser", savedUser.getUsername());
        assertEquals("testuser@mail.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
    }

    @Test
    void registerUserThrowsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.registerUser(request));

        assertEquals("Error: El nombre de usuario ya está en uso", ex.getMessage());
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("testuser@mail.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.registerUser(request));

        assertEquals("Error: El correo electrónico ya está registrado", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordUpdatesPasswordWhenCurrentPasswordIsValid() {
        User user = new User("testuser", "testuser@mail.com", "old-hash");
        user.setId(8L);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password-123");
        request.setConfirmNewPassword("new-password-123");

        when(userRepository.findById(8L)).thenReturn(java.util.Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.matches("new-password-123", "old-hash")).thenReturn(false);
        when(encoder.encode("new-password-123")).thenReturn("new-hash");

        ResponseEntity<?> response = authService.changePassword(8L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("message"));
        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(user.getUpdatedAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordThrowsWhenConfirmationDoesNotMatch() {
        User user = new User("testuser", "testuser@mail.com", "old-hash");
        user.setId(8L);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password-123");
        request.setConfirmNewPassword("different-new-password");

        when(userRepository.findById(8L)).thenReturn(java.util.Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.changePassword(8L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordThrowsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        ChangePasswordRequest request = new ChangePasswordRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.changePassword(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordIncorrect() {
        User user = new User("testuser", "testuser@mail.com", "old-hash");
        user.setId(8L);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-password");

        when(userRepository.findById(8L)).thenReturn(java.util.Optional.of(user));
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.changePassword(8L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void changePasswordThrowsWhenNewPasswordEqualsOld() {
        User user = new User("testuser", "testuser@mail.com", "old-hash");
        user.setId(8L);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("old-password");
        request.setConfirmNewPassword("old-password");

        when(userRepository.findById(8L)).thenReturn(java.util.Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> authService.changePassword(8L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void loginReturnsOkWhenCredentialsAreValid() {
        com.easyschedule.backend.auth.dto.request.LoginRequest loginRequest = new com.easyschedule.backend.auth.dto.request.LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("password123");

        User user = new User("testuser", "testuser@mail.com", "hashed-password");
        user.setId(1L);

        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(java.util.Optional.of(user));
        when(encoder.matches("password123", "hashed-password")).thenReturn(true);
        when(sessionTokenService.issueToken(1L)).thenReturn("mock-token");
        when(sessionTokenService.getTokenTtlSeconds()).thenReturn(3600L);

        ResponseEntity<?> response = authService.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("mock-token", body.get("token"));
        assertEquals("testuser", body.get("username"));
    }

    @Test
    void loginReturnsUnauthorizedWhenUserNotFound() {
        com.easyschedule.backend.auth.dto.request.LoginRequest loginRequest = new com.easyschedule.backend.auth.dto.request.LoginRequest();
        loginRequest.setIdentifier("unknown");

        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase("unknown")).thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = authService.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void loginReturnsUnauthorizedWhenPasswordIsIncorrect() {
        com.easyschedule.backend.auth.dto.request.LoginRequest loginRequest = new com.easyschedule.backend.auth.dto.request.LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("wrong");

        User user = new User("testuser", "testuser@mail.com", "hashed-password");

        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(java.util.Optional.of(user));
        when(encoder.matches("wrong", "hashed-password")).thenReturn(false);

        ResponseEntity<?> response = authService.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void logoutRevokesToken() {
        authService.logout("Bearer my-token");
        verify(sessionTokenService).revokeToken("my-token");
    }

    @Test
    void logoutDoesNothingWhenHeaderIsInvalid() {
        authService.logout(null);
        verify(sessionTokenService).revokeToken("");

        authService.logout("InvalidHeader");
        verify(sessionTokenService, org.mockito.Mockito.atLeastOnce()).revokeToken("");
    }
}
