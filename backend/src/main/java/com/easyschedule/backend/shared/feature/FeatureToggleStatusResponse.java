package com.easyschedule.backend.shared.feature;

public record FeatureToggleStatusResponse(
    String key,
    boolean active
) {
}
