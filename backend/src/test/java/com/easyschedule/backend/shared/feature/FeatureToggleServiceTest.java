package com.easyschedule.backend.shared.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    @Mock
    private FeatureToggleRepository featureToggleRepository;

    @Test
    void updateStatusRefreshesUpdatedAt() {
        FeatureToggleService service = new FeatureToggleService(
            featureToggleRepository,
            new FeatureFlagsConfig(true, true)
        );
        OffsetDateTime previousUpdatedAt = OffsetDateTime.now().minusDays(1);
        FeatureToggle toggle = new FeatureToggle(
            "malla",
            "Malla curricular",
            "Controla el acceso a mallas.",
            true
        );
        toggle.setUpdatedAt(previousUpdatedAt);

        when(featureToggleRepository.existsByKey(anyString())).thenReturn(true);
        when(featureToggleRepository.findByKey("malla")).thenReturn(Optional.of(toggle));
        when(featureToggleRepository.save(any(FeatureToggle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeatureToggleResponse response = service.updateStatus("malla", false);

        ArgumentCaptor<FeatureToggle> savedToggle = ArgumentCaptor.forClass(FeatureToggle.class);
        verify(featureToggleRepository).save(savedToggle.capture());

        assertFalse(response.active());
        assertFalse(savedToggle.getValue().isActive());
        assertTrue(response.updatedAt().isAfter(previousUpdatedAt));
    }
}
