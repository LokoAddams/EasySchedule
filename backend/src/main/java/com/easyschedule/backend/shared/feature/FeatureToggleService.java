package com.easyschedule.backend.shared.feature;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeatureToggleService {



    private final FeatureToggleRepository featureToggleRepository;
    private final FeatureFlagsConfig featureFlagsConfig;

    public FeatureToggleService(FeatureToggleRepository featureToggleRepository, FeatureFlagsConfig featureFlagsConfig) {
        this.featureToggleRepository = featureToggleRepository;
        this.featureFlagsConfig = featureFlagsConfig;
    }

    @Transactional
    public FeatureFlagsDTO getFeatureFlags() {
        ensureDefaultToggles();
        return new FeatureFlagsDTO();
    }

    @Transactional
    public List<FeatureToggleResponse> findAll() {
        ensureDefaultToggles();
        return featureToggleRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FeatureToggleResponse findByKey(String key) {
        ensureDefaultToggles();
        return toResponse(findExistingByKey(key));
    }

    @Transactional
    public FeatureToggleStatusResponse findStatusByKey(String key) {
        ensureDefaultToggles();
        FeatureToggle toggle = findExistingByKey(key);
        return new FeatureToggleStatusResponse(toggle.getKey(), toggle.isActive());
    }

    @Transactional
    public FeatureToggleResponse updateStatus(String key, boolean active) {
        ensureDefaultToggles();
        FeatureToggle toggle = findExistingByKey(key);
        toggle.setActive(active);
        toggle.setUpdatedAt(OffsetDateTime.now());
        return toResponse(featureToggleRepository.save(toggle));
    }

    @Transactional
    public boolean isEnabled(String key) {
        ensureDefaultToggles();
        return findExistingByKey(key).isActive();
    }

    private void ensureDefaultToggles() {
        for (FeatureToggleDefinition definition : defaultToggles()) {
            if (!featureToggleRepository.existsByKey(definition.key())) {
                featureToggleRepository.save(new FeatureToggle(
                    definition.key(),
                    definition.name(),
                    definition.description(),
                    definition.active()
                ));
            }
        }
    }

    private FeatureToggle findExistingByKey(String key) {
        String normalizedKey = normalizeKey(key);
        return featureToggleRepository.findByKey(normalizedKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature toggle no encontrado"));
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave del feature toggle es requerida");
        }

        return key.trim();
    }

    private FeatureToggleResponse toResponse(FeatureToggle toggle) {
        return new FeatureToggleResponse(
            toggle.getKey(),
            toggle.getName(),
            toggle.getDescription(),
            toggle.isActive(),
            toggle.getUpdatedAt()
        );
    }

    private List<FeatureToggleDefinition> defaultToggles() {
        return List.of();
    }

    private record FeatureToggleDefinition(String key, String name, String description, boolean active) {}
}
