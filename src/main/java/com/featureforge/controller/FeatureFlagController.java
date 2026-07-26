package com.featureforge.controller;

import com.featureforge.domain.User;
import com.featureforge.dto.*;
import com.featureforge.service.FeatureFlagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @PostMapping("/api/v1/projects/{projectId}/flags")
    public ResponseEntity<FeatureFlagResponse> create(@PathVariable UUID projectId,
                                                       @Valid @RequestBody CreateFeatureFlagRequest request,
                                                       @AuthenticationPrincipal User user) {
        FeatureFlagResponse response = featureFlagService.create(projectId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/projects/{projectId}/flags")
    public List<FeatureFlagResponse> listForProject(@PathVariable UUID projectId,
                                                     @AuthenticationPrincipal User user) {
        return featureFlagService.listForProject(projectId, user.getId());
    }

    @GetMapping("/api/v1/flags/{flagId}")
    public FeatureFlagResponse get(@PathVariable UUID flagId,
                                   @AuthenticationPrincipal User user) {
        return featureFlagService.get(flagId, user.getId());
    }

    @PatchMapping("/api/v1/flags/{flagId}")
    public FeatureFlagResponse update(@PathVariable UUID flagId,
                                      @Valid @RequestBody UpdateFeatureFlagRequest request,
                                      @AuthenticationPrincipal User user) {
        return featureFlagService.update(flagId, request, user.getId());
    }

    @DeleteMapping("/api/v1/flags/{flagId}")
    public ResponseEntity<Void> delete(@PathVariable UUID flagId,
                                       @AuthenticationPrincipal User user) {
        featureFlagService.delete(flagId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/flags/{flagId}/evaluate")
    public EvaluateFlagResponse evaluate(@PathVariable UUID flagId,
                                         @Valid @RequestBody EvaluateFlagRequest request,
                                         @AuthenticationPrincipal User user) {
        return featureFlagService.evaluate(flagId, request.targetingKey(), user.getId());
    }

    @PutMapping("/api/v1/flags/{flagId}/overrides")
    public ResponseEntity<Void> setOverride(@PathVariable UUID flagId,
                                            @Valid @RequestBody SetOverrideRequest request,
                                            @AuthenticationPrincipal User user) {
        featureFlagService.setOverride(flagId, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/flags/{flagId}/overrides/{targetingKey}")
    public ResponseEntity<Void> removeOverride(@PathVariable UUID flagId,
                                               @PathVariable String targetingKey,
                                               @AuthenticationPrincipal User user) {
        featureFlagService.removeOverride(flagId, targetingKey, user.getId());
        return ResponseEntity.noContent().build();
    }
}
