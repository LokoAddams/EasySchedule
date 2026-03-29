package com.easyschedule.backend.estudiante.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.auth.dto.RegistroRequest;
import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.auth.service.AuthService;
import com.easyschedule.backend.estudiante.dto.EstudianteUpdateRequest;
import com.easyschedule.backend.estudiante.dto.PerfilUpdateRequest;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.easyschedule.backend.malla.model.Malla;
import com.easyschedule.backend.malla.repository.MallaRepository;
import com.easyschedule.backend.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private MallaRepository mallaRepository;

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EstudianteService estudianteService;

    @Test
    void registerCreatesProfileWithIncompleteFlag() {
        RegistroRequest request = new RegistroRequest("nuevo_usuario", "clave123", "nuevo@mail.com");
        User createdUser = new User("nuevo_usuario", "nuevo@mail.com", "hashed");
        createdUser.setId(10L);

        when(userRepository.findByUsername("nuevo_usuario")).thenReturn(Optional.of(createdUser));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(invocation -> {
            Estudiante saved = invocation.getArgument(0);
            saved.setId(saved.getUser().getId());
            return saved;
        });

        var response = estudianteService.register(request);

        verify(authService).registerUser(any());
        assertEquals(10L, response.id());
        assertFalse(response.profileCompleted());
        assertEquals("nuevo_usuario", response.username());
    }

    @Test
    void updateMarksProfileAsCompletedWhenRequiredFieldsExist() {
        User user = new User("usuario", "usuario@mail.com", "hashed");
        user.setId(20L);

        Malla malla = new Malla();
        malla.setId(3L);

        Estudiante profile = new Estudiante();
        profile.setId(20L);
        profile.setUser(user);
        profile.setFechaRegistro(OffsetDateTime.now());

        EstudianteUpdateRequest request = new EstudianteUpdateRequest(
            "Juan",
            "Perez",
            "1234567",
            LocalDate.of(2000, 1, 1),
            (short) 3,
            1L,
            2L,
            3L
        );

        when(estudianteRepository.findById(20L)).thenReturn(Optional.of(profile));
        when(mallaRepository.findById(3L)).thenReturn(Optional.of(malla));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = estudianteService.update(20L, request);

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).save(captor.capture());
        assertTrue(captor.getValue().isProfileCompleted());
        assertTrue(response.profileCompleted());
        assertEquals(1L, response.universidadId());
        assertEquals(2L, response.carreraId());
    }

    @Test
    void findByUsernameReturnsProfileWhenUserExists() {
        Estudiante estudiante = buildProfile("diego", "diego@mail.com");
        when(estudianteRepository.findByUsernameIgnoreCase("diego")).thenReturn(Optional.of(estudiante));

        var response = estudianteService.findByUsername("diego");

        assertEquals("diego", response.username());
        assertEquals("diego@mail.com", response.email());
    }

    @Test
    void findByUsernameThrowsWhenUserDoesNotExist() {
        when(estudianteRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> estudianteService.findByUsername("missing"));
    }

    @Test
    void updateProfileUpdatesUserAndProfileData() {
        User user = new User("diego", "diego@mail.com", "hashed");
        user.setId(30L);

        Estudiante estudiante = buildProfile("diego", "diego@mail.com");
        estudiante.setId(30L);
        estudiante.setUser(user);

        PerfilUpdateRequest request = new PerfilUpdateRequest(
            "diego2",
            "Diego",
            "Suarez",
            "diego2@mail.com",
            "998877",
            LocalDate.of(2001, 5, 10),
            "",
            ""
        );

        when(estudianteRepository.findByUsernameIgnoreCase("diego")).thenReturn(Optional.of(estudiante));
        when(userRepository.existsByUsername("diego2")).thenReturn(false);
        when(userRepository.existsByEmail("diego2@mail.com")).thenReturn(false);
        when(estudianteRepository.existsByCarnetIdentidadIgnoreCase("998877")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = estudianteService.updateProfile("diego", request);

        assertEquals("diego2", response.username());
        assertEquals("diego2@mail.com", response.email());
        assertEquals("Diego", response.nombre());
        assertEquals("Suarez", response.apellido());
        assertEquals("998877", response.carnetIdentidad());
        verify(userRepository).save(any(User.class));
        verify(estudianteRepository).save(any(Estudiante.class));
    }

    private Estudiante buildProfile(String username, String email) {
        Estudiante profile = new Estudiante();
        profile.setUsername(username);
        profile.setCorreo(email);
        profile.setFechaRegistro(OffsetDateTime.now());
        profile.setProfileCompleted(false);
        return profile;
    }
}
