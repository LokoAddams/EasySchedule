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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EstudianteService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteService.class);

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

        if (estudiante.getUser() != null) {
            if (estudiante.getUsername() == null || estudiante.getUsername().isBlank()) {
                estudiante.setUsername(estudiante.getUser().getUsername());
            }
            if (estudiante.getCorreo() == null || estudiante.getCorreo().isBlank()) {
                estudiante.setCorreo(estudiante.getUser().getEmail());
            }
        }

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

        Estudiante estudianteSaved = estudianteRepository.save(estudiante);
        log.info("[ESTUDIANTE_REGISTRO] Perfil de estudiante creado para el usuario: {} con ID: {}", user.getUsername(), estudianteSaved.getId());
        return toResponse(estudianteSaved);
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
            && estudiante.getUsername() != null
            && !estudiante.getUsername().isBlank()
            && estudiante.getCorreo() != null
            && !estudiante.getCorreo().isBlank()
            && estudiante.getCarnetIdentidad() != null
            && !estudiante.getCarnetIdentidad().isBlank()
            && estudiante.getFechaNacimiento() != null;
    }

    private EstudianteResponse toResponse(Estudiante estudiante) {
        Long mallaId = estudiante.getMalla() != null ? estudiante.getMalla().getId() : null;
        String username = estudiante.getUsername();
        String email = estudiante.getCorreo();

        if ((username == null || username.isBlank()) && estudiante.getUser() != null) {
            username = estudiante.getUser().getUsername();
        }

        if ((email == null || email.isBlank()) && estudiante.getUser() != null) {
            email = estudiante.getUser().getEmail();
        }

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
        return toResponse(getOrCreateByIdentifier(username));
    }

    public boolean canAccessProfile(String identifier, Long userId) {
        return userRepository.findByUsernameIgnoreCase(identifier)
            .or(() -> userRepository.findByEmailIgnoreCase(identifier))
            .map(User::getId)
            .filter((id) -> id.equals(userId))
            .isPresent();
    }

    @Transactional
    public EstudianteResponse updateProfile(String username, PerfilUpdateRequest request) {
        Estudiante estudiante = getOrCreateByIdentifier(username);

        User user = estudiante.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "El estudiante no tiene un usuario asociado");
        }

        String usernameNormalizado = request.username().trim();
        String emailNormalizado = request.email().trim().toLowerCase();
        String carnetNormalizado = request.carnetIdentidad().trim();

        if (!user.getUsername().equalsIgnoreCase(usernameNormalizado)
            && (Boolean.TRUE.equals(userRepository.existsByUsernameIgnoreCase(usernameNormalizado))
                || estudianteRepository.existsByUsernameIgnoreCase(usernameNormalizado))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: El nombre de usuario ya está en uso");
        }

        if (!user.getEmail().equalsIgnoreCase(emailNormalizado)
            && (Boolean.TRUE.equals(userRepository.existsByEmailIgnoreCase(emailNormalizado))
                || estudianteRepository.existsByCorreoIgnoreCase(emailNormalizado))) {
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

    private Estudiante getOrCreateByIdentifier(String identifier) {
        User user = userRepository.findByUsernameIgnoreCase(identifier)
            .or(() -> userRepository.findByEmailIgnoreCase(identifier))
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario no encontrado con username o correo: " + identifier
            ));

        Optional<Estudiante> existing = estudianteRepository.findById(user.getId());
        if (existing.isPresent()) {
            Estudiante estudiante = existing.get();
            boolean changed = false;

            if (estudiante.getUser() == null) {
                estudiante.setUser(user);
                changed = true;
            }

            if (estudiante.getUsername() == null || !estudiante.getUsername().equalsIgnoreCase(user.getUsername())) {
                estudiante.setUsername(user.getUsername());
                changed = true;
            }

            if (estudiante.getCorreo() == null || !estudiante.getCorreo().equalsIgnoreCase(user.getEmail())) {
                estudiante.setCorreo(user.getEmail());
                changed = true;
            }

            return changed ? estudianteRepository.save(estudiante) : estudiante;
        }

        Estudiante estudiante = new Estudiante();
        estudiante.setUsername(user.getUsername());
        estudiante.setCorreo(user.getEmail());
        estudiante.setFechaRegistro(OffsetDateTime.now());
        estudiante.setProfileCompleted(false);
        estudiante.setUser(user);

        try {
            return estudianteRepository.save(estudiante);
        } catch (RuntimeException ex) {
            return estudianteRepository.findByUsernameIgnoreCase(user.getUsername())
                .or(() -> estudianteRepository.findByCorreoIgnoreCase(user.getEmail()))
                .orElseThrow(() -> ex);
        }
    }
}
