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
@ConditionalOnBean(FeatureToggleService.class)
public class FeatureToggleInterceptor implements HandlerInterceptor {

    private final FeatureToggleService featureToggleService;

    public FeatureToggleInterceptor(FeatureToggleService featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }
}
