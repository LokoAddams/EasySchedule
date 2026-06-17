package com.easyschedule.backend.shared.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagsService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagsService.class);

    private final FeatureFlagsConfig featureFlagsConfig;

    public FeatureFlagsService(FeatureFlagsConfig featureFlagsConfig) {
        this.featureFlagsConfig = featureFlagsConfig;
    }

    public FeatureFlagsDTO getFeatureFlags() {
        try {
            return new FeatureFlagsDTO(
                featureFlagsConfig.isMalla(),
                featureFlagsConfig.isTomaMaterias(),
                featureFlagsConfig.isOfertasImport()
            );
        } catch (RuntimeException ex) {
            FeatureFlagsDTO fallback = FeatureFlagsDefaults.safeDefaults();
            log.warn(
                "[FEATURE_TOGGLES_FALLBACK] No se pudo leer la configuracion de feature toggles. "
                    + "Aplicando defaults seguros: malla={}, tomaMaterias={}, ofertasImport={}",
                fallback.malla(),
                fallback.tomaMaterias(),
                fallback.ofertasImport(),
                ex
            );
            return fallback;
        }
    }
}
