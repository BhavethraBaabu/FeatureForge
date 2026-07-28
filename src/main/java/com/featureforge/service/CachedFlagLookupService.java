package com.featureforge.service;

import com.featureforge.dto.FlagCacheEntry;
import com.featureforge.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Single chokepoint for the Redis-backed flag cache, same idea as
 * AccessControlService centralizing role checks. Two things read through
 * this: the SDK bootstrap endpoint (all flags for a project) and the SDK
 * single-flag evaluate endpoint. Nothing on the dashboard/CRUD side reads
 * through the cache — those screens want immediate consistency, not a
 * cached view, since an admin just clicked "save" and expects to see it.
 */
@Service
@RequiredArgsConstructor
public class CachedFlagLookupService {

    private final FeatureFlagRepository featureFlagRepository;

    @Cacheable(cacheNames = "flagByProjectAndKey", key = "#projectId + ':' + #flagKey")
    public Optional<FlagCacheEntry> findByProjectAndKey(UUID projectId, String flagKey) {
        return featureFlagRepository.findByProjectIdAndKey(projectId, flagKey)
                .map(FlagCacheEntry::fromEntity);
    }

    @Cacheable(cacheNames = "flagsByProject", key = "#projectId")
    public List<FlagCacheEntry> findAllForProject(UUID projectId) {
        return featureFlagRepository.findByProjectId(projectId).stream()
                .map(FlagCacheEntry::fromEntity)
                .toList();
    }

    /**
     * Called by FeatureFlagService after every create/update/delete. Evicts
     * both the single-flag entry and the project-wide list — the list has
     * to go too, since it's stale the moment any flag in it changes shape.
     */
    @CacheEvict(cacheNames = "flagByProjectAndKey", key = "#projectId + ':' + #flagKey")
    public void evictFlag(UUID projectId, String flagKey) {
        // no-op body; annotation does the work
    }

    @CacheEvict(cacheNames = "flagsByProject", key = "#projectId")
    public void evictProjectList(UUID projectId) {
        // no-op body; annotation does the work
    }

    public void evictAfterMutation(UUID projectId, String flagKey) {
        evictFlag(projectId, flagKey);
        evictProjectList(projectId);
    }
}
