package com.easyschedule.backend.auth.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.easyschedule.backend.auth.models.ERole;
import com.easyschedule.backend.auth.models.Role;
import com.easyschedule.backend.auth.models.User;
import com.easyschedule.backend.auth.repositories.RoleRepository;
import com.easyschedule.backend.auth.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role roleUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleUser = roleRepository.findByName(ERole.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_USER)));
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerEndpointCreatesUserAndAssignsRole() throws Exception {
        String requestBody = """
                {
                  "username": "integration_user",
                  "email": "integration_user@mail.com",
                  "password": "123456",
                  "role": ["user"]
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        User createdUser = userRepository.findByUsername("integration_user").orElseThrow();

        assertNotNull(createdUser.getId());
        assertTrue(passwordEncoder.matches("123456", createdUser.getPassword()));
        assertTrue(createdUser.getRoles().stream().map(Role::getName).anyMatch(name -> name == ERole.ROLE_USER));
    }

    @Test
    void registerEndpointReturnsConflictWhenUsernameAlreadyExists() throws Exception {
        User existing = new User("integration_user", "first@mail.com", passwordEncoder.encode("123456"));
        existing.setRoles(Set.of(roleUser));
        userRepository.save(existing);

        String requestBody = """
                {
                  "username": "integration_user",
                  "email": "second@mail.com",
                  "password": "123456",
                  "role": ["user"]
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Error: El nombre de usuario ya está en uso"));
    }
}
