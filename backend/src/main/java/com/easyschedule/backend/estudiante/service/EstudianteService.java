package com.easyschedule.backend.estudiante.service;

import com.easyschedule.backend.auth.dto.RegistroRequest;
import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.auth.service.AuthService;
import com.easyschedule.backend.estudiante.dto.EstudianteResponse;
import com.easyschedule.backend.estudiante.dto.EstudianteUpdateRequest;
import com.easyschedule.backend.estudiante.dto.PerfilUpdateRequest;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.easyschedule.backend.malla.model.Malla;
import com.easyschedule.backend.malla.repository.MallaRepository;
import com.easyschedule.backend.shared.exception.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final MallaRepository mallaRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    public EstudianteService(
        EstudianteRepository estudianteRepository,
        MallaRepository mallaRepository,
        AuthService authService,
        UserRepository userRepository
    ) {
        this.estudianteRepository = estudianteRepository;
        this.mallaRepository = mallaRepository;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    public List<EstudianteResponse> findAll() {
        return estudianteRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EstudianteResponse findById(Long id) {
        return toResponse(getEstudianteOrThrow(id));
    }

    public EstudianteResponse update(Long id, EstudianteUpdateRequest request) {
        Estudiante estudiante = getEstudianteOrThrow(id);
        Malla malla = request.mallaId() == null ? null : getMallaOrThrow(request.mallaId());

        estudiante.setNombre(request.nombre());
        estudiante.setApellido(request.apellido());
        estudiante.setCarnetIdentidad(request.carnetIdentidad());
        estudiante.setFechaNacimiento(request.fechaNacimiento());
        estudiante.setSemestreActual(request.semestreActual());
        estudiante.setUniversidadId(request.universidadId());
        estudiante.setCarreraId(request.carreraId());
        estudiante.setMalla(malla);
        estudiante.setProfileCompleted(isProfileCompleted(estudiante));

        return toResponse(estudianteRepository.save(estudiante));
    }

    public void delete(Long id) {
        Estudiante estudiante = getEstudianteOrThrow(id);
        estudianteRepository.delete(estudiante);
    }

    @Transactional
    public EstudianteResponse register(RegistroRequest request) {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(request.username());
        signupRequest.setEmail(request.email());
        signupRequest.setPassword(request.password());

        authService.registerUser(signupRequest);

        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el usuario recien registrado"));

        Estudiante estudiante = new Estudiante();
        estudiante.setUsername(user.getUsername());
        estudiante.setCorreo(user.getEmail());
        estudiante.setFechaRegistro(OffsetDateTime.now());
        estudiante.setProfileCompleted(false);
        estudiante.setUser(user);

        return toResponse(estudianteRepository.save(estudiante));
    }

    private Estudiante getEstudianteOrThrow(Long id) {
        return estudianteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
    }

    private Malla getMallaOrThrow(Long id) {
        return mallaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Malla no encontrada con id: " + id));
    }

    private boolean isProfileCompleted(Estudiante estudiante) {
        return estudiante.getNombre() != null
            && !estudiante.getNombre().isBlank()
            && estudiante.getApellido() != null
            && !estudiante.getApellido().isBlank()
            && estudiante.getCarnetIdentidad() != null
            && !estudiante.getCarnetIdentidad().isBlank()
            && estudiante.getFechaNacimiento() != null
            && estudiante.getSemestreActual() != null
            && estudiante.getUniversidadId() != null
            && estudiante.getCarreraId() != null
            && Objects.nonNull(estudiante.getMalla());
    }

    private EstudianteResponse toResponse(Estudiante estudiante) {
        Long mallaId = estudiante.getMalla() != null ? estudiante.getMalla().getId() : null;
        String username = estudiante.getUsername();
        String email = estudiante.getCorreo();

        return new EstudianteResponse(
            estudiante.getId(),
            username,
            estudiante.getNombre(),
            estudiante.getApellido(),
            email,
            estudiante.getCarnetIdentidad(),
            estudiante.getFechaNacimiento(),
            estudiante.getFechaRegistro(),
            estudiante.getSemestreActual(),
            estudiante.getUniversidadId(),
            estudiante.getCarreraId(),
            mallaId,
            estudiante.isProfileCompleted()
        );
    }
    public EstudianteResponse findByUsername(String username) {
        Estudiante estudiante = estudianteRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con username: " + username));

        return toResponse(estudiante);
    }

    @Transactional
    public EstudianteResponse updateProfile(String username, PerfilUpdateRequest request) {
        Estudiante estudiante = estudianteRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con username: " + username));

        User user = estudiante.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "El estudiante no tiene un usuario asociado");
        }

        String usernameNormalizado = request.username().trim();
        String emailNormalizado = request.email().trim().toLowerCase();
        String carnetNormalizado = request.carnetIdentidad().trim();

        if (!user.getUsername().equalsIgnoreCase(usernameNormalizado) && userRepository.existsByUsername(usernameNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: El nombre de usuario ya está en uso");
        }

        if (!user.getEmail().equalsIgnoreCase(emailNormalizado) && userRepository.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: El correo electrónico ya está registrado");
        }

        String carnetActual = estudiante.getCarnetIdentidad() == null ? "" : estudiante.getCarnetIdentidad();
        if (!carnetActual.equalsIgnoreCase(carnetNormalizado)
            && estudianteRepository.existsByCarnetIdentidadIgnoreCase(carnetNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: El carnet de identidad ya está en uso");
        }

        user.setUsername(usernameNormalizado);
        user.setEmail(emailNormalizado);

        estudiante.setUsername(usernameNormalizado);
        estudiante.setCorreo(emailNormalizado);
        estudiante.setNombre(request.nombre().trim());
        estudiante.setApellido(request.apellido().trim());
        estudiante.setCarnetIdentidad(carnetNormalizado);
        estudiante.setFechaNacimiento(request.fechaNacimiento());
        estudiante.setProfileCompleted(isProfileCompleted(estudiante));

        userRepository.save(user);
        Estudiante estudianteActualizado = estudianteRepository.save(estudiante);
        return toResponse(estudianteActualizado);
    }
}