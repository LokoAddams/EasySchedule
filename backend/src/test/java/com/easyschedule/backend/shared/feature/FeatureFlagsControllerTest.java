package com.easyschedule.backend.shared.feature;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.easyschedule.backend.shared.config.BearerTokenAuthenticationFilter;
import com.easyschedule.backend.shared.admin.AdminAccessService;
import com.easyschedule.backend.shared.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeatureFlagsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FeatureFlagsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureToggleService featureToggleService;

    @MockitoBean
    private BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @Test
    void getFeatureFlagsReturnsCurrentFlags() throws Exception {
        when(featureToggleService.getFeatureFlags()).thenReturn(new FeatureFlagsDTO());

        mockMvc.perform(get("/api/features"))
            .andExpect(status().isOk());
    }

    @Test
    void getFeatureTogglesReturnsAvailableToggles() throws Exception {
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        when(featureToggleService.findAll()).thenReturn(List.of(
            new FeatureToggleResponse("malla", "Malla curricular", "Modulo de mallas", true, updatedAt)
        ));

        mockMvc.perform(get("/api/features/toggles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].key").value("malla"))
            .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getFeatureToggleStatusReturnsOneToggleState() throws Exception {
        when(featureToggleService.findStatusByKey("malla"))
            .thenReturn(new FeatureToggleStatusResponse("malla", true));

        mockMvc.perform(get("/api/features/toggles/malla/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("malla"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateFeatureToggleChangesState() throws Exception {
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        when(featureToggleService.updateStatus("malla", false))
            .thenReturn(new FeatureToggleResponse("malla", "Malla curricular", "Modulo de mallas", false, updatedAt));

        mockMvc.perform(patch("/api/features/toggles/malla")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("malla"))
            .andExpect(jsonPath("$.active").value(false));

        verify(featureToggleService).updateStatus("malla", false);
    }

    @Test
    void updateFeatureToggleReturnsBadRequestWhenStateIsMissing() throws Exception {
        mockMvc.perform(patch("/api/features/toggles/malla")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
