
package com.easyschedule.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.User;

import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.auth.dto.request.ChangePasswordRequest;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;
import java.util.Optional;
import java.util.Map;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.easyschedule.backend.auth.dto.request.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final SessionTokenService sessionTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder encoder, SessionTokenService sessionTokenService) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.sessionTokenService = sessionTokenService;
    }

    public void registerUser(SignupRequest signUpRequest) {
        String normalizedUsername = signUpRequest.getUsername().trim();
        String normalizedEmail = signUpRequest.getEmail().trim().toLowerCase();

        if (Boolean.TRUE.equals(userRepository.existsByUsernameIgnoreCase(normalizedUsername))) {
           throw new UserAlreadyExistsException("Error: El nombre de usuario ya está en uso");        }

        if (Boolean.TRUE.equals(userRepository.existsByEmailIgnoreCase(normalizedEmail))) {
            throw new UserAlreadyExistsException("Error: El correo electrónico ya está registrado");        }

        User user = new User(
            normalizedUsername,
            normalizedEmail,
            encoder.encode(signUpRequest.getPassword())
        );
    
        
        userRepository.save(user);
    }
    public ResponseEntity<?> login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        log.info("[AUTH_LOGIN] intento autenticacion | identifier={}", identifier);

        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(identifier)
            .or(() -> userRepository.findByEmailIgnoreCase(identifier));

        if (userOpt.isEmpty()) {
            log.warn("[AUTH_LOGIN] fallo autenticacion | identifier={} motivo=usuario_no_encontrado", identifier);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas");
        }
    
        User user = userOpt.get();
    

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[AUTH_LOGIN] fallo autenticacion | userId={} motivo=password_incorrecta", user.getId());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas");
        }

        String token = sessionTokenService.issueToken(user.getId());
        log.info("[AUTH_LOGIN] autenticacion exitosa | userId={} username={}", user.getId(), user.getUsername());

        return ResponseEntity.ok().body(
            Map.of(
                "token", token,
                "username", user.getUsername(),
                "message", "Login exitoso"
            )
        );
    }

    public ResponseEntity<?> logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        sessionTokenService.revokeToken(token);
        return ResponseEntity.ok().body(Map.of("message", "Sesion cerrada correctamente"));
    }

    public ResponseEntity<?> changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    

        if (!encoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("[AUTH_CHANGE_PASSWORD] fallo | userId={} | reason=CURRENT_PASSWORD_INCORRECT", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasenia actual es incorrecta");
        }
    

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            log.warn("[AUTH_CHANGE_PASSWORD] fallo | userId={} | reason=PASSWORD_CONFIRMATION_MISMATCH", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contrasenia y su confirmacion no coinciden");
        }
    

        if (encoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            log.warn("[AUTH_CHANGE_PASSWORD] fallo | userId={} | reason=NEW_PASSWORD_EQUALS_OLD", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contrasenia debe ser diferente a la actual");
        }
    

        user.setPasswordHash(encoder.encode(request.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    
        log.info("[AUTH_CHANGE_PASSWORD] exito | userId={}", userId);
    
        return ResponseEntity.ok().body(Map.of("message", "Contrasenia actualizada correctamente"));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return "";
        }

        return authorizationHeader.substring(7).trim();
    }
}
