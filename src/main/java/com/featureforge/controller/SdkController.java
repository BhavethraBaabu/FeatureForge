package com.featureforge.controller;

import com.featureforge.dto.EvaluateFlagResponse;
import com.featureforge.dto.FlagCacheEntry;
import com.featureforge.dto.SdkEvaluateAllRequest;
import com.featureforge.dto.SdkEvaluateRequest;
import com.featureforge.dto.SdkFlagBootstrapResponse;
import com.featureforge.exception.ResourceNotFoundException;
import com.featureforge.security.ApiKeyAuthFilter;
import com.featureforge.service.CachedFlagLookupService;
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
 * ApiKeyAuthFilter before this controller runs.
 *
 * Reads through CachedFlagLookupService (Redis), not FeatureFlagRepository
 * directly — this is the high-QPS path (every SDK-instrumented app polling
 * or evaluating on every request), so it shouldn't hit Postgres per call.
 */
@RestController
@RequestMapping("/api/v1/sdk")
@RequiredArgsConstructor
public class SdkController {

    private final CachedFlagLookupService cachedFlagLookupService;
    private final RolloutEngine rolloutEngine;

    @GetMapping("/flags")
    public List<SdkFlagBootstrapResponse> bootstrap(
            @RequestAttribute(ApiKeyAuthFilter.RESOLVED_PROJECT_ID_ATTR) UUID resolvedProjectId) {

        return cachedFlagLookupService.findAllForProject(resolvedProjectId).stream()
                .map(f -> new SdkFlagBootstrapResponse(f.key(), f.enabled(), f.rolloutPercentage()))
                .toList();
    }

    @PostMapping("/evaluate")
    public EvaluateFlagResponse evaluate(
            @RequestAttribute(ApiKeyAuthFilter.RESOLVED_PROJECT_ID_ATTR) UUID resolvedProjectId,
            @Valid @RequestBody SdkEvaluateRequest request) {

        FlagCacheEntry flag = cachedFlagLookupService
                .findByProjectAndKey(resolvedProjectId, request.flagKey())
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag", request.flagKey()));

        return rolloutEngine.evaluate(flag, request.targetingKey());
    }

    @PostMapping("/evaluate-all")
    public Map<String, EvaluateFlagResponse> evaluateAll(
            @RequestAttribute(ApiKeyAuthFilter.RESOLVED_PROJECT_ID_ATTR) UUID resolvedProjectId,
            @Valid @RequestBody SdkEvaluateAllRequest request) {

        return cachedFlagLookupService.findAllForProject(resolvedProjectId).stream()
                .collect(Collectors.toMap(
                        FlagCacheEntry::key,
                        flag -> rolloutEngine.evaluate(flag, request.targetingKey())));
    }
}
