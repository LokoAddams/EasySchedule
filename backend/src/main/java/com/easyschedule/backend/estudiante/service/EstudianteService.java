package com.easyschedule.backend.estudiante.service;

import com.easyschedule.backend.auth.dto.RegistroRequest;
import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.auth.service.AuthService;
import com.easyschedule.backend.estudiante.dto.EstudianteResponse;
import com.easyschedule.backend.estudiante.dto.EstudianteUpdateRequest;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.easyschedule.backend.malla.model.Malla;
import com.easyschedule.backend.malla.repository.MallaRepository;
import com.easyschedule.backend.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EstudianteService {

    private static final String DEFAULT_CARRERA = "carrera default";
    private static final String DEFAULT_UNIVERSIDAD = "universidad default";
    private static final String DEFAULT_VERSION = "version default";

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

    public EstudianteResponse findByUsername(String username) {
        Estudiante estudiante = estudianteRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con username: " + username));

        return toResponse(estudiante);
    }

    public EstudianteResponse update(Long id, EstudianteUpdateRequest request) {
        Estudiante estudiante = getEstudianteOrThrow(id);
        Malla malla = getMallaOrThrow(request.mallaId());

        estudiante.setNombre(request.nombre());
        estudiante.setApellido(request.apellido());
        estudiante.setCarnetIdentidad(request.carnetIdentidad());
        estudiante.setFechaNacimiento(request.fechaNacimiento());
        estudiante.setSemestreActual(request.semestreActual());
        estudiante.setCarrera(request.carrera());
        estudiante.setMalla(malla);

        return toResponse(estudianteRepository.save(estudiante));
    }

    public void delete(Long id) {
        Estudiante estudiante = getEstudianteOrThrow(id);
        estudianteRepository.delete(estudiante);
    }

    @Transactional
    public EstudianteResponse register(RegistroRequest request) {
        Malla malla = mallaRepository
            .findByCarreraIgnoreCaseAndUniversidadIgnoreCaseAndVersionIgnoreCase(
                DEFAULT_CARRERA,
                DEFAULT_UNIVERSIDAD,
                DEFAULT_VERSION
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No existe la malla default. Ejecuta los scripts SQL manuales para crearla."
            ));

        String nombreDefault = "Nombre por defecto";
        String apellidoDefault = "Apellido por defecto";
        String carnetIdentidadDefault = "CI-" + request.username();
        LocalDate fechaNacimientoDefault = LocalDate.of(2000, 1, 1);
        short semestreActualDefault = 1;

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(request.username());
        signupRequest.setEmail(request.email());
        signupRequest.setPassword(request.password());

        authService.registerUser(signupRequest);

        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo recuperar el usuario recién registrado"));

        Estudiante estudiante = new Estudiante();
        estudiante.setUsername(user.getUsername());
        estudiante.setCorreo(user.getEmail());
        estudiante.setPasswordHash(user.getPassword());
        estudiante.setNombre(nombreDefault);
        estudiante.setApellido(apellidoDefault);
        estudiante.setCarnetIdentidad(carnetIdentidadDefault);
        estudiante.setFechaNacimiento(fechaNacimientoDefault);
        estudiante.setFechaRegistro(OffsetDateTime.now());
        estudiante.setSemestreActual(semestreActualDefault);
        estudiante.setCarrera(DEFAULT_CARRERA);
        estudiante.setMalla(malla);
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

    private EstudianteResponse toResponse(Estudiante estudiante) {
        Long mallaId = estudiante.getMalla() != null ? estudiante.getMalla().getId() : null;
        String universidad = estudiante.getMalla() != null ? estudiante.getMalla().getUniversidad() : null;
        String username = estudiante.getUser() != null ? estudiante.getUser().getUsername() : null;
        String email = estudiante.getUser() != null ? estudiante.getUser().getEmail() : null;

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
            estudiante.getCarrera(),
            mallaId,
            universidad
        );
    }
}