package com.easyschedule.backend.shared.feature;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/features")
public class FeatureFlagsController {

    private final FeatureFlagsConfig featureFlagsConfig;

    public FeatureFlagsController(FeatureFlagsConfig featureFlagsConfig) {
        this.featureFlagsConfig = featureFlagsConfig;
    }

    @GetMapping
    public FeatureFlagsDTO getFeatureFlags() {
        return new FeatureFlagsDTO(featureFlagsConfig.isMalla(), featureFlagsConfig.isTomaMaterias());
    }
}
