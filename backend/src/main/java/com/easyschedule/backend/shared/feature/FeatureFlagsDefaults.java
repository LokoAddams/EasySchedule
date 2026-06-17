package com.easyschedule.backend.shared.feature;

public final class FeatureFlagsDefaults {

    public static final boolean MALLA_ENABLED = true;
    public static final boolean TOMA_MATERIAS_ENABLED = true;
    public static final boolean OFERTAS_IMPORT_ENABLED = false;

    private FeatureFlagsDefaults() {
    }

    public static FeatureFlagsDTO safeDefaults() {
        return new FeatureFlagsDTO(
            MALLA_ENABLED,
            TOMA_MATERIAS_ENABLED,
            OFERTAS_IMPORT_ENABLED
        );
    }
}
