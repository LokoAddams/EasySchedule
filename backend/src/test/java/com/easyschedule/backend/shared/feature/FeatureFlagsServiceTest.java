package com.easyschedule.backend.shared.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class FeatureFlagsServiceTest {

    @Test
    void returnsConfiguredFlagsWhenConfigIsAvailable() {
        FeatureFlagsConfig config = mock(FeatureFlagsConfig.class);
        when(config.isMalla()).thenReturn(false);
        when(config.isTomaMaterias()).thenReturn(true);
        when(config.isOfertasImport()).thenReturn(false);

        FeatureFlagsDTO flags = new FeatureFlagsService(config).getFeatureFlags();

        assertFalse(flags.malla());
        assertTrue(flags.tomaMaterias());
        assertFalse(flags.ofertasImport());
    }

    @Test
    void returnsSafeDefaultsWhenConfigReadFails() {
        FeatureFlagsConfig config = mock(FeatureFlagsConfig.class);
        when(config.isMalla()).thenThrow(new IllegalStateException("toggle source unavailable"));

        FeatureFlagsDTO flags = new FeatureFlagsService(config).getFeatureFlags();

        assertTrue(flags.malla());
        assertTrue(flags.tomaMaterias());
        assertFalse(flags.ofertasImport());
    }

    @Test
    void retriesConfigAfterFallbackOnNextRead() {
        FeatureFlagsConfig config = mock(FeatureFlagsConfig.class);
        when(config.isMalla())
            .thenThrow(new IllegalStateException("temporary failure"))
            .thenReturn(false);
        when(config.isTomaMaterias()).thenReturn(false);
        when(config.isOfertasImport()).thenReturn(true);

        FeatureFlagsService service = new FeatureFlagsService(config);

        FeatureFlagsDTO fallback = service.getFeatureFlags();
        FeatureFlagsDTO recovered = service.getFeatureFlags();

        assertTrue(fallback.malla());
        assertTrue(fallback.tomaMaterias());
        assertFalse(fallback.ofertasImport());
        assertFalse(recovered.malla());
        assertFalse(recovered.tomaMaterias());
        assertTrue(recovered.ofertasImport());
    }
}
