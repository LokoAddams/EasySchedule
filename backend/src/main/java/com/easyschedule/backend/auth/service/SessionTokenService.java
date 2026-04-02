package com.easyschedule.backend.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SessionTokenService {

    private static final Logger log = LoggerFactory.getLogger(SessionTokenService.class);

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

        log.info(
            "[SESSION_TOKEN] emitido | userId={} tokenRef={} expiresAt={}",
            userId,
            tokenRef(token),
            expiresAt
        );
        return token;
    }

    public Optional<Long> validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            log.warn("[SESSION_TOKEN] validacion invalida | motivo=token_vacio");
            return Optional.empty();
        }

        SessionTokenRecord record = tokenStore.get(token);
        if (record == null) {
            log.warn("[SESSION_TOKEN] validacion invalida | tokenRef={} motivo=token_no_encontrado", tokenRef(token));
            return Optional.empty();
        }

        if (record.expiresAt().isBefore(Instant.now())) {
            tokenStore.remove(token);
            log.warn(
                "[SESSION_TOKEN] token expirado | userId={} tokenRef={} expiresAt={}",
                record.userId(),
                tokenRef(token),
                record.expiresAt()
            );
            return Optional.empty();
        }

        log.info("[SESSION_TOKEN] validacion exitosa | userId={} tokenRef={}", record.userId(), tokenRef(token));
        return Optional.of(record.userId());
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("[SESSION_TOKEN] revocacion ignorada | motivo=token_vacio");
            return;
        }

        SessionTokenRecord removed = tokenStore.remove(token);
        if (removed == null) {
            log.warn("[SESSION_TOKEN] revocacion sin efecto | tokenRef={} motivo=token_no_encontrado", tokenRef(token));
            return;
        }

        log.info("[SESSION_TOKEN] revocado | userId={} tokenRef={}", removed.userId(), tokenRef(token));
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokenStore.entrySet().removeIf((entry) -> {
            boolean expired = entry.getValue().expiresAt().isBefore(now);
            if (expired) {
                SessionTokenRecord record = entry.getValue();
                log.info(
                    "[SESSION_TOKEN] token expirado limpiado | userId={} tokenRef={} expiresAt={}",
                    record.userId(),
                    tokenRef(entry.getKey()),
                    record.expiresAt()
                );
            }
            return expired;
        });
    }

    private String tokenRef(String token) {
        if (token == null || token.isBlank()) {
            return "n/a";
        }

        if (token.length() <= 10) {
            return token;
        }

        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    private record SessionTokenRecord(Long userId, Instant expiresAt) {}
}
