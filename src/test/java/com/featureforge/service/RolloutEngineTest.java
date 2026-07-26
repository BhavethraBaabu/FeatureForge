package com.featureforge.service;

import com.featureforge.domain.FeatureFlag;
import com.featureforge.domain.FlagOverride;
import com.featureforge.dto.EvaluateFlagResponse;
import com.featureforge.repository.FlagOverrideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolloutEngineTest {

    @Mock
    private FlagOverrideRepository flagOverrideRepository;

    @InjectMocks
    private RolloutEngine rolloutEngine;

    private FeatureFlag flagWith(boolean enabled, int rolloutPercentage) {
        return FeatureFlag.builder()
                .id(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .key("new-checkout")
                .name("New Checkout")
                .enabled(enabled)
                .rolloutPercentage((short) rolloutPercentage)
                .build();
    }

    @Test
    void masterKillSwitch_alwaysReturnsFalse_regardlessOfRollout() {
        FeatureFlag flag = flagWith(false, 100);

        EvaluateFlagResponse response = rolloutEngine.evaluate(flag, "user-123");

        assertThat(response.enabled()).isFalse();
        assertThat(response.reason()).isEqualTo("FLAG_DISABLED");
    }

    @Test
    void override_takesPrecedenceOverRolloutPercentage() {
        FeatureFlag flag = flagWith(true, 0);
        FlagOverride override = FlagOverride.builder()
                .flagId(flag.getId())
                .targetingKey("vip-user")
                .enabled(true)
                .build();

        when(flagOverrideRepository.findByFlagIdAndTargetingKey(flag.getId(), "vip-user"))
                .thenReturn(Optional.of(override));

        EvaluateFlagResponse response = rolloutEngine.evaluate(flag, "vip-user");

        assertThat(response.enabled()).isTrue();
        assertThat(response.reason()).isEqualTo("OVERRIDE");
    }

    @Test
    void rolloutAt100Percent_alwaysReturnsTrue_forAnyTargetingKey() {
        FeatureFlag flag = flagWith(true, 100);
        when(flagOverrideRepository.findByFlagIdAndTargetingKey(any(), any())).thenReturn(Optional.empty());

        for (int i = 0; i < 50; i++) {
            EvaluateFlagResponse response = rolloutEngine.evaluate(flag, "user-" + i);
            assertThat(response.enabled()).isTrue();
        }
    }

    @Test
    void rolloutAt0Percent_alwaysReturnsFalse_forAnyTargetingKey() {
        FeatureFlag flag = flagWith(true, 0);
        when(flagOverrideRepository.findByFlagIdAndTargetingKey(any(), any())).thenReturn(Optional.empty());

        for (int i = 0; i < 50; i++) {
            EvaluateFlagResponse response = rolloutEngine.evaluate(flag, "user-" + i);
            assertThat(response.enabled()).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"user-1", "user-2", "user-42", "user-999"})
    void evaluation_isSticky_sameTargetingKeyAlwaysGetsSameResult(String targetingKey) {
        FeatureFlag flag = flagWith(true, 50);
        when(flagOverrideRepository.findByFlagIdAndTargetingKey(any(), any())).thenReturn(Optional.empty());

        boolean first = rolloutEngine.evaluate(flag, targetingKey).enabled();
        boolean second = rolloutEngine.evaluate(flag, targetingKey).enabled();
        boolean third = rolloutEngine.evaluate(flag, targetingKey).enabled();

        assertThat(first).isEqualTo(second).isEqualTo(third);
    }

    @Test
    void rolloutAt50Percent_distributesRoughlyEvenly_acrossManyTargetingKeys() {
        FeatureFlag flag = flagWith(true, 50);
        when(flagOverrideRepository.findByFlagIdAndTargetingKey(any(), any())).thenReturn(Optional.empty());

        long enabledCount = 0;
        int sampleSize = 1000;
        for (int i = 0; i < sampleSize; i++) {
            if (rolloutEngine.evaluate(flag, "user-" + i).enabled()) {
                enabledCount++;
            }
        }

        assertThat(enabledCount).isBetween((long) (sampleSize * 0.35), (long) (sampleSize * 0.65));
    }
}
