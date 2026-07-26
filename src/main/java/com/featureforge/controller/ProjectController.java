package com.featureforge.controller;

import com.featureforge.domain.User;
import com.featureforge.dto.CreateProjectRequest;
import com.featureforge.dto.ProjectResponse;
import com.featureforge.service.ProjectService;
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
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/api/v1/organizations/{organizationId}/projects")
    public ResponseEntity<ProjectResponse> create(@PathVariable UUID organizationId,
                                                   @Valid @RequestBody CreateProjectRequest request,
                                                   @AuthenticationPrincipal User user) {
        ProjectResponse response = projectService.create(organizationId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/organizations/{organizationId}/projects")
    public List<ProjectResponse> listForOrganization(@PathVariable UUID organizationId,
                                                      @AuthenticationPrincipal User user) {
        return projectService.listForOrganization(organizationId, user.getId());
    }

    @GetMapping("/api/v1/projects/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId,
                               @AuthenticationPrincipal User user) {
        return projectService.get(projectId, user.getId());
    }
}
