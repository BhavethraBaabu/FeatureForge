package com.featureforge.repository;

import com.featureforge.domain.FlagOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlagOverrideRepository extends JpaRepository<FlagOverride, UUID> {

    List<FlagOverride> findByFlagId(UUID flagId);

    Optional<FlagOverride> findByFlagIdAndTargetingKey(UUID flagId, String targetingKey);

    void deleteByFlagIdAndTargetingKey(UUID flagId, String targetingKey);
}
