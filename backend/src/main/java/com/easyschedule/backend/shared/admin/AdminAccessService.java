package com.easyschedule.backend.shared.admin;

import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAccessService {

    private final UserRepository userRepository;

    public AdminAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void requireAdmin(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion requerida");
        }

        Long userId;
        try {
            userId = Long.valueOf(principal.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida"));

        if (!AdminUser.isAdmin(user)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el usuario administrador puede administrar feature toggles"
            );
        }
    }
}
