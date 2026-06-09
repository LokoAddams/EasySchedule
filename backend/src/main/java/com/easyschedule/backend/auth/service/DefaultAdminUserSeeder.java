package com.easyschedule.backend.auth.service;

import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.UserRepository;
import com.easyschedule.backend.shared.admin.AdminUser;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultAdminUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DefaultAdminUserSeeder(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.admin.seed.enabled:true}") boolean enabled
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        String passwordHash = passwordEncoder.encode(AdminUser.DEFAULT_PASSWORD);
        userRepository.findByEmailIgnoreCase(AdminUser.EMAIL)
            .map((user) -> updateExistingAdmin(user, passwordHash))
            .orElseGet(() -> userRepository.findByUsernameIgnoreCase(AdminUser.USERNAME)
                .map((user) -> updateExistingAdmin(user, passwordHash))
                .orElseGet(() -> createAdmin(passwordHash)));
    }

    private User updateExistingAdmin(User user, String passwordHash) {
        user.setUsername(AdminUser.USERNAME);
        user.setEmail(AdminUser.EMAIL);
        user.setPasswordHash(passwordHash);
        user.setActive(true);
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    private User createAdmin(String passwordHash) {
        User admin = new User(AdminUser.USERNAME, AdminUser.EMAIL, passwordHash);
        admin.setActive(true);
        return userRepository.save(admin);
    }
}
