package com.easyschedule.backend.shared.feature;

import java.time.OffsetDateTime;

public record FeatureToggleResponse(
    String key,
    String name,
    String description,
    boolean active,
    OffsetDateTime updatedAt
) {
}
