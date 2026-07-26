package com.featureforge.service;

import com.featureforge.domain.FeatureFlag;
import com.featureforge.domain.FlagOverride;
import com.featureforge.domain.OrgRole;
import com.featureforge.domain.Project;
import com.featureforge.dto.*;
import com.featureforge.exception.DuplicateResourceException;
import com.featureforge.exception.ResourceNotFoundException;
import com.featureforge.repository.FeatureFlagRepository;
import com.featureforge.repository.FlagOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;
    private final FlagOverrideRepository flagOverrideRepository;
    private final ProjectService projectService;
    private final AccessControlService accessControlService;
    private final RolloutEngine rolloutEngine;

    @Transactional
    public FeatureFlagResponse create(UUID projectId, CreateFeatureFlagRequest request, UUID requesterId) {
        Project project = projectService.findByIdOrThrow(projectId);
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        if (featureFlagRepository.existsByProjectIdAndKey(projectId, request.key())) {
            throw new DuplicateResourceException(
                    "A flag with key '%s' already exists in this project".formatted(request.key()));
        }

        FeatureFlag flag = FeatureFlag.builder()
                .projectId(projectId)
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .enabled(request.enabled())
                .rolloutPercentage(request.rolloutPercentage())
                .createdBy(requesterId)
                .build();
        featureFlagRepository.save(flag);

        log.info("Flag '{}' created in project {} (enabled={}, rollout={}%)",
                flag.getKey(), projectId, flag.isEnabled(), flag.getRolloutPercentage());
        return FeatureFlagResponse.fromEntity(flag);
    }

    public List<FeatureFlagResponse> listForProject(UUID projectId, UUID requesterId) {
        Project project = projectService.findByIdOrThrow(projectId);
        accessControlService.requireMembership(project.getOrganizationId(), requesterId);

        return featureFlagRepository.findByProjectId(projectId).stream()
                .map(FeatureFlagResponse::fromEntity)
                .toList();
    }

    public FeatureFlagResponse get(UUID flagId, UUID requesterId) {
        FeatureFlag flag = findByIdOrThrow(flagId);
        Project project = projectService.findByIdOrThrow(flag.getProjectId());
        accessControlService.requireMembership(project.getOrganizationId(), requesterId);
        return FeatureFlagResponse.fromEntity(flag);
    }

    @Transactional
    public FeatureFlagResponse update(UUID flagId, UpdateFeatureFlagRequest request, UUID requesterId) {
        FeatureFlag flag = findByIdOrThrow(flagId);
        Project project = projectService.findByIdOrThrow(flag.getProjectId());
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        if (request.name() != null) flag.setName(request.name());
        if (request.description() != null) flag.setDescription(request.description());
        if (request.enabled() != null) flag.setEnabled(request.enabled());
        if (request.rolloutPercentage() != null) flag.setRolloutPercentage(request.rolloutPercentage());

        featureFlagRepository.save(flag);
        log.info("Flag '{}' updated by user {}", flag.getKey(), requesterId);
        return FeatureFlagResponse.fromEntity(flag);
    }

    @Transactional
    public void delete(UUID flagId, UUID requesterId) {
        FeatureFlag flag = findByIdOrThrow(flagId);
        Project project = projectService.findByIdOrThrow(flag.getProjectId());
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        featureFlagRepository.delete(flag);
        log.info("Flag '{}' deleted by user {}", flag.getKey(), requesterId);
    }

    public EvaluateFlagResponse evaluate(UUID flagId, String targetingKey, UUID requesterId) {
        FeatureFlag flag = findByIdOrThrow(flagId);
        Project project = projectService.findByIdOrThrow(flag.getProjectId());
        accessControlService.requireMembership(project.getOrganizationId(), requesterId);

        return rolloutEngine.evaluate(flag, targetingKey);
    }

    @Transactional
    public void setOverride(UUID flagId, SetOverrideRequest request, UUID requesterId) {
        FeatureFlag flag = findByIdOrThrow(flagId);
        Project project = projectService.findByIdOrThrow(flag.getProjectId());
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        FlagOverride override = flagOverrideRepository
                .findByFlagIdAndTargetingKey(flagId, request.targetingKey())
                .map(existing -> {
                    existing.setEnabled(request.enabled());
                    return existing;
                })
                .orElseGet(() -> FlagOverride.builder()
                        .flagId(flagId)
                        .targetingKey(request.targetingKey())
                        .enabled(request.enabled())
                        .build());

        flagOverrideRepository.save(override);
        log.info("Override set for flag '{}' target '{}' -> {}", flag.getKey(), request.targetingKey(), request.enabled());
    }

    @Transactional
    public void removeOverride(UUID flagId, String targetingKey, UUID requesterId) {
        FeatureFlag flag = findByIdOrThrow(flagId);
        Project project = projectService.findByIdOrThrow(flag.getProjectId());
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        flagOverrideRepository.deleteByFlagIdAndTargetingKey(flagId, targetingKey);
    }

    private FeatureFlag findByIdOrThrow(UUID flagId) {
        return featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag", flagId));
    }
}
