package com.easyschedule.backend.shared.feature;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/features")
public class FeatureFlagsController {

    private final FeatureFlagsService featureFlagsService;

    public FeatureFlagsController(FeatureFlagsService featureFlagsService) {
        this.featureFlagsService = featureFlagsService;
    }

    @GetMapping
    public FeatureFlagsDTO getFeatureFlags() {
        return featureFlagsService.getFeatureFlags();
    }
}
