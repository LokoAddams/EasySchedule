package com.easyschedule.backend.shared.feature;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagsConfig {

    private final boolean malla;
    private final boolean ofertasImport;

    public FeatureFlagsConfig(
        @Value("${features.malla:true}") boolean malla,
        @Value("${features.ofertas-import:true}") boolean ofertasImport
    ) {
        this.malla = malla;
        this.ofertasImport = ofertasImport;
    }

    public boolean isMalla() {
        return malla;
    }

    public boolean isOfertasImport() {
        return ofertasImport;
    }
}