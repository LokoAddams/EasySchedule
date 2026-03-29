package com.easyschedule.backend.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class SessionTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(8);

    private final ConcurrentMap<String, SessionTokenRecord> tokenStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public String issueToken(Long userId) {
        cleanupExpiredTokens();

        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);
        tokenStore.put(token, new SessionTokenRecord(userId, expiresAt));
        return token;
    }

    public Optional<Long> validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        SessionTokenRecord record = tokenStore.get(token);
        if (record == null) {
            return Optional.empty();
        }

        if (record.expiresAt().isBefore(Instant.now())) {
            tokenStore.remove(token);
            return Optional.empty();
        }

        return Optional.of(record.userId());
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        tokenStore.remove(token);
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokenStore.entrySet().removeIf((entry) -> entry.getValue().expiresAt().isBefore(now));
    }

    private record SessionTokenRecord(Long userId, Instant expiresAt) {}
}
