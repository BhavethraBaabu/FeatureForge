package com.featureforge.service;

import com.featureforge.domain.OrgRole;
import com.featureforge.domain.Project;
import com.featureforge.dto.CreateProjectRequest;
import com.featureforge.dto.ProjectResponse;
import com.featureforge.exception.DuplicateResourceException;
import com.featureforge.exception.ResourceNotFoundException;
import com.featureforge.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final AccessControlService accessControlService;

    @Transactional
    public ProjectResponse create(UUID organizationId, CreateProjectRequest request, UUID requesterId) {
        accessControlService.requireRole(organizationId, requesterId, OrgRole.ADMIN);

        if (projectRepository.existsByOrganizationIdAndKey(organizationId, request.key())) {
            throw new DuplicateResourceException(
                    "A project with key '%s' already exists in this organization".formatted(request.key()));
        }

        Project project = Project.builder()
                .organizationId(organizationId)
                .name(request.name())
                .key(request.key())
                .build();
        projectRepository.save(project);

        log.info("Project '{}' created in organization {}", project.getKey(), organizationId);
        return ProjectResponse.fromEntity(project);
    }

    public List<ProjectResponse> listForOrganization(UUID organizationId, UUID requesterId) {
        accessControlService.requireMembership(organizationId, requesterId);
        return projectRepository.findByOrganizationId(organizationId).stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    public ProjectResponse get(UUID projectId, UUID requesterId) {
        Project project = findByIdOrThrow(projectId);
        accessControlService.requireMembership(project.getOrganizationId(), requesterId);
        return ProjectResponse.fromEntity(project);
    }

    Project findByIdOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }
}
