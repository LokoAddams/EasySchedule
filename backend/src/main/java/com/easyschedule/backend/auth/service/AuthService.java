
package com.easyschedule.backend.auth.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.easyschedule.backend.auth.dto.request.SignupRequest;
import com.easyschedule.backend.auth.models.ERole;
import com.easyschedule.backend.auth.models.Role;
import com.easyschedule.backend.auth.models.User;

import com.easyschedule.backend.auth.repositories.RoleRepository;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.shared.exception.UserAlreadyExistsException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
    }

    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
           throw new UserAlreadyExistsException("Error: El nombre de usuario ya está en uso");        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new UserAlreadyExistsException("Error: El correo electrónico ya está registrado");        }

        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<Role> roles = new HashSet<>();

        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró el rol solicitado."));
        roles.add(userRole);

        user.setRoles(roles);
        
        userRepository.save(user);
    }
}
