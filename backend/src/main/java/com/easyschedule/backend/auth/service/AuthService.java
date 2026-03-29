
package com.easyschedule.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.User;

import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.easyschedule.backend.auth.dto.request.LoginRequest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
           throw new UserAlreadyExistsException("Error: El nombre de usuario ya está en uso");        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new UserAlreadyExistsException("Error: El correo electrónico ya está registrado");        }

        User user = new User(
            signUpRequest.getUsername(),
            signUpRequest.getEmail(),
            encoder.encode(signUpRequest.getPassword())
        );
    
        
        userRepository.save(user);
    }
    public ResponseEntity<?> login(LoginRequest request) {


        if (request.getIdentifier() == null || request.getPassword() == null ||
            request.getIdentifier().isEmpty() || request.getPassword().isEmpty()) {
    
            return ResponseEntity
                    .badRequest()
                    .body("Todos los campos son obligatorios");
        }
    

        Optional<User> userOpt = userRepository
                .findByUsernameOrEmail(
                        request.getIdentifier(),
                        request.getIdentifier()
                );
    

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
    

        String token = UUID.randomUUID().toString();
    
        return ResponseEntity.ok().body(
                java.util.Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "message", "Login exitoso"
                )
        );
    }
}
