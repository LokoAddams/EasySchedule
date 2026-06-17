package com.easyschedule.backend.shared.feature;

import com.easyschedule.backend.shared.admin.AdminAccessService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/features")
public class FeatureFlagsController {

    private final FeatureToggleService featureToggleService;
    private final AdminAccessService adminAccessService;

    public FeatureFlagsController(FeatureToggleService featureToggleService, AdminAccessService adminAccessService) {
        this.featureToggleService = featureToggleService;
        this.adminAccessService = adminAccessService;
    }

    @GetMapping
    public FeatureFlagsDTO getFeatureFlags() {
        return featureToggleService.getFeatureFlags();
    }

    @GetMapping("/toggles")
    public List<FeatureToggleResponse> getFeatureToggles(Principal principal) {
        adminAccessService.requireAdmin(principal);
        return featureToggleService.findAll();
    }

    @GetMapping("/toggles/{key}")
    public FeatureToggleResponse getFeatureToggle(@PathVariable("key") String key, Principal principal) {
        adminAccessService.requireAdmin(principal);
        return featureToggleService.findByKey(key);
    }

    @GetMapping("/toggles/{key}/status")
    public FeatureToggleStatusResponse getFeatureToggleStatus(@PathVariable("key") String key, Principal principal) {
        adminAccessService.requireAdmin(principal);
        return featureToggleService.findStatusByKey(key);
    }

    @PatchMapping("/toggles/{key}")
    public FeatureToggleResponse updateFeatureToggle(
        @PathVariable("key") String key,
        @Valid @RequestBody FeatureToggleUpdateRequest request,
        Principal principal
    ) {
        adminAccessService.requireAdmin(principal);
        return featureToggleService.updateStatus(key, request.active());
    }
}
