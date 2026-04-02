package com.easyschedule.backend.shared.config;

import com.easyschedule.backend.auth.service.SessionTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenAuthenticationFilter.class);

    private final SessionTokenService sessionTokenService;

    public BearerTokenAuthenticationFilter(SessionTokenService sessionTokenService) {
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            Optional<Long> userId = sessionTokenService.validateAndGetUserId(token);

            if (userId.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    String.valueOf(userId.get()),
                    null,
                    Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info(
                    "[BEARER_FILTER] token valido | userId={} method={} path={}",
                    userId.get(),
                    request.getMethod(),
                    request.getRequestURI()
                );
            } else if (userId.isPresent()) {
                log.info(
                    "[BEARER_FILTER] token valido sin reemplazo de contexto | userId={} method={} path={}",
                    userId.get(),
                    request.getMethod(),
                    request.getRequestURI()
                );
            } else {
                log.warn(
                    "[BEARER_FILTER] token invalido | method={} path={} tokenRef={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    tokenRef(token)
                );
            }
        }

        filterChain.doFilter(request, response);
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
}
