package com.featureforge.controller;

import com.featureforge.domain.FeatureFlag;
import com.featureforge.dto.EvaluateFlagResponse;
import com.featureforge.dto.SdkEvaluateAllRequest;
import com.featureforge.dto.SdkEvaluateRequest;
import com.featureforge.dto.SdkFlagBootstrapResponse;
import com.featureforge.exception.ResourceNotFoundException;
import com.featureforge.repository.FeatureFlagRepository;
import com.featureforge.security.ApiKeyAuthFilter;
import com.featureforge.service.RolloutEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SDK-facing endpoints. Auth is X-API-Key, handled entirely by
 * ApiKeyAuthFilter before this controller runs — the resolved project id
 * arrives as a request attribute, never as a path/body param a caller
 * could tamper with independently of their key.
 *
 * Uncached for now: this hits FeatureFlagRepository directly. Redis caching
 * of flag definitions lands as its own piece.
 */
@RestController
@RequestMapping("/api/v1/sdk")
@RequiredArgsConstructor
public class SdkController {

    private final FeatureFlagRepository featureFlagRepository;
    private final RolloutEngine rolloutEngine;

    @GetMapping("/flags")
    public List<SdkFlagBootstrapResponse> bootstrap(
            @RequestAttribute(ApiKeyAuthFilter.RESOLVED_PROJECT_ID_ATTR) UUID resolvedProjectId) {

        return featureFlagRepository.findByProjectId(resolvedProjectId).stream()
                .map(SdkFlagBootstrapResponse::fromEntity)
                .toList();
    }

    @PostMapping("/evaluate")
    public EvaluateFlagResponse evaluate(
            @RequestAttribute(ApiKeyAuthFilter.RESOLVED_PROJECT_ID_ATTR) UUID resolvedProjectId,
            @Valid @RequestBody SdkEvaluateRequest request) {

        FeatureFlag flag = featureFlagRepository.findByProjectIdAndKey(resolvedProjectId, request.flagKey())
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag", request.flagKey()));

        return rolloutEngine.evaluate(flag, request.targetingKey());
    }

    @PostMapping("/evaluate-all")
    public Map<String, EvaluateFlagResponse> evaluateAll(
            @RequestAttribute(ApiKeyAuthFilter.RESOLVED_PROJECT_ID_ATTR) UUID resolvedProjectId,
            @Valid @RequestBody SdkEvaluateAllRequest request) {

        return featureFlagRepository.findByProjectId(resolvedProjectId).stream()
                .collect(Collectors.toMap(
                        FeatureFlag::getKey,
                        flag -> rolloutEngine.evaluate(flag, request.targetingKey())));
    }
}
