package com.easyschedule.backend.auth.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.easyschedule.backend.auth.models.User;
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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerEndpointCreatesUser() throws Exception {
        String requestBody = """
                {
                  "username": "integration_user",
                  "email": "integration_user@mail.com",
                  "password": "Abcd1234!"
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        User createdUser = userRepository.findByUsername("integration_user").orElseThrow();

        assertNotNull(createdUser.getId());
        assertTrue(passwordEncoder.matches("Abcd1234!", createdUser.getPasswordHash()));
    }

    @Test
    void registerEndpointReturnsConflictWhenUsernameAlreadyExists() throws Exception {
        User existing = new User("integration_user", "first@mail.com", passwordEncoder.encode("Abcd1234!"));
        userRepository.save(existing);

        String requestBody = """
                {
                  "username": "integration_user",
                  "email": "second@mail.com",
                  "password": "Abcd1234!"
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Error: El nombre de usuario ya está en uso"));
    }

    @Test
    void registerEndpointReturnsBadRequestWhenPasswordIsWeak() throws Exception {
        String requestBody = """
                {
                  "username": "integration_user",
                  "email": "integration_user@mail.com",
                  "password": "abcd1234"
                }
                """;

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
