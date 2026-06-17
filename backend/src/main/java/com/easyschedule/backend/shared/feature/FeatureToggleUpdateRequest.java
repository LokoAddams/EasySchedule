package com.easyschedule.backend.shared.feature;

import jakarta.validation.constraints.NotNull;

public record FeatureToggleUpdateRequest(
    @NotNull(message = "El estado del feature toggle es requerido")
    Boolean active
) {
}
