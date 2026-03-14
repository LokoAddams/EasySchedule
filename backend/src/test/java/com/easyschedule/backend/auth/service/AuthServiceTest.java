package com.easyschedule.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.ERole;
import com.easyschedule.backend.auth.models.Role;
import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.RoleRepository;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

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
    void registerUserSavesUserWithEncodedPasswordAndRole() {
        Role roleUser = new Role(ERole.ROLE_USER);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@mail.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("encoded-password");
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(roleUser));

        authService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("testuser", savedUser.getUsername());
        assertEquals("testuser@mail.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertTrue(savedUser.getRoles().contains(roleUser));
    }

    @Test
    void registerUserThrowsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.registerUser(request));

        assertEquals("Error: El nombre de usuario ya está en uso", ex.getMessage());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@mail.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.registerUser(request));

        assertEquals("Error: El correo electrónico ya está registrado", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserThrowsWhenRoleUserDoesNotExist() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@mail.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("encoded-password");
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.registerUser(request));

        assertTrue(ex.getMessage().contains("No se encontró el rol solicitado"));
        verify(userRepository, never()).save(any());
    }
}
