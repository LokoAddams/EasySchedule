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

}
