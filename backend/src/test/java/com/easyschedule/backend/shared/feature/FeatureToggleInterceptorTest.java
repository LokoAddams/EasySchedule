package com.easyschedule.backend.shared.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class FeatureToggleInterceptorTest {

    @Test
    void allowsCoreAcademicPathWhenFallbackDefaultsAreApplied() {
        FeatureFlagsService service = mock(FeatureFlagsService.class);
        when(service.getFeatureFlags()).thenReturn(FeatureFlagsDefaults.safeDefaults());
        FeatureToggleInterceptor interceptor = new FeatureToggleInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/academico/mallas");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
    }

    @Test
    void blocksOfferImportPathWhenFallbackDefaultsAreApplied() {
        FeatureFlagsService service = mock(FeatureFlagsService.class);
        when(service.getFeatureFlags()).thenReturn(FeatureFlagsDefaults.safeDefaults());
        FeatureToggleInterceptor interceptor = new FeatureToggleInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/academico/ofertas/importar");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
