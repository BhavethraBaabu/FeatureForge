package com.featureforge.dto;

import com.featureforge.domain.FeatureFlag;

public record SdkFlagBootstrapResponse(
        String key,
        boolean enabled,
        short rolloutPercentage
) {
    public static SdkFlagBootstrapResponse fromEntity(FeatureFlag flag) {
        return new SdkFlagBootstrapResponse(flag.getKey(), flag.isEnabled(), flag.getRolloutPercentage());
    }
}
