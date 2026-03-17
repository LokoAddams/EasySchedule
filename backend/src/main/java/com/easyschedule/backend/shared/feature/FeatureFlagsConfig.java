package com.easyschedule.backend.shared.feature;

import org.springframework.context.annotation.Configuration;

@Configuration
public class FeatureFlagsConfig {

    // Backend-owned defaults without external property files.
    private final boolean malla = false;
    private final boolean tomaMaterias = false;

    public boolean isMalla() {
        return malla;
    }

    public boolean isTomaMaterias() {
        return tomaMaterias;
    }
}
