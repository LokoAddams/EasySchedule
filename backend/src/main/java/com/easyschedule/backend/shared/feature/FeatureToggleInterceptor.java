package com.easyschedule.backend.shared.feature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnBean(FeatureFlagsService.class)
public class FeatureToggleInterceptor implements HandlerInterceptor {

    private static final List<String> MALLA_PATH_PREFIXES = List.of(
        "/api/academico/universidades",
        "/api/academico/carreras",
        "/api/academico/mallas",
        "/api/academico/seleccion",
        "/api/academico/estados-materia",
        "/api/estudiantes/me/avance-graduacion",
        "/api/academico/prerequisitos"
    );

    private static final List<String> OFERTAS_IMPORT_PATH_PREFIXES = List.of(
        "/api/academico/ofertas/importar"
    );

    private static final List<String> TOMA_MATERIAS_PATH_PREFIXES = List.of(
        "/api/academico/toma-materias",
        "/api/academico/horario"
    );

    private final FeatureFlagsService featureFlagsService;

    public FeatureToggleInterceptor(FeatureFlagsService featureFlagsService) {
        this.featureFlagsService = featureFlagsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();
        FeatureFlagsDTO featureFlags = featureFlagsService.getFeatureFlags();

        if (!featureFlags.malla() && matchesAny(requestPath, MALLA_PATH_PREFIXES)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La funcionalidad de malla esta deshabilitada");
        }

        if (!featureFlags.ofertasImport() && matchesAny(requestPath, OFERTAS_IMPORT_PATH_PREFIXES)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La importacion de ofertas esta deshabilitada");
        }

        if (!featureFlags.tomaMaterias() && matchesAny(requestPath, TOMA_MATERIAS_PATH_PREFIXES)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La funcionalidad de toma de materias esta deshabilitada");
        }

        return true;
    }

    private boolean matchesAny(String requestPath, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (requestPath.equals(prefix) || requestPath.startsWith(prefix + "/")) {
                return true;
            }
        }

        return false;
    }
}
