package com.featureforge.repository;

import com.featureforge.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    List<FeatureFlag> findByProjectId(UUID projectId);

    Optional<FeatureFlag> findByProjectIdAndKey(UUID projectId, String key);

    boolean existsByProjectIdAndKey(UUID projectId, String key);
}
