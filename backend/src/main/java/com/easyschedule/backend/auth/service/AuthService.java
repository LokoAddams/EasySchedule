
package com.easyschedule.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.User;

import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;
import java.util.Optional;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.easyschedule.backend.auth.dto.request.LoginRequest;

@Service
public class AuthService {

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

        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(identifier)
            .or(() -> userRepository.findByEmailIgnoreCase(identifier));

        if (userOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas");
        }
    
        User user = userOpt.get();
    

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas");
        }

        String token = sessionTokenService.issueToken(user.getId());

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

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return "";
        }

        return authorizationHeader.substring(7).trim();
    }
}
