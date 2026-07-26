package com.featureforge.repository;

import com.featureforge.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOrganizationId(UUID organizationId);

    Optional<Project> findByOrganizationIdAndKey(UUID organizationId, String key);

    boolean existsByOrganizationIdAndKey(UUID organizationId, String key);
}
