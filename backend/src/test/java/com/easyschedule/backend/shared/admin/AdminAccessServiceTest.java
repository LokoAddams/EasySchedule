package com.easyschedule.backend.shared.admin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void requireAdminAllowsConfiguredAdminUser() {
        AdminAccessService service = new AdminAccessService(userRepository);
        User admin = new User(AdminUser.USERNAME, AdminUser.EMAIL, "hash");
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> service.requireAdmin(() -> "1"));
    }

    @Test
    void requireAdminRejectsRegularUsers() {
        AdminAccessService service = new AdminAccessService(userRepository);
        User user = new User("student", "student@mail.com", "hash");
        user.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.requireAdmin(() -> "2")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
