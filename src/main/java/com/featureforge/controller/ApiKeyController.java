package com.featureforge.controller;

import com.featureforge.domain.User;
import com.featureforge.dto.ApiKeyCreatedResponse;
import com.featureforge.dto.ApiKeyResponse;
import com.featureforge.dto.CreateApiKeyRequest;
import com.featureforge.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> create(@PathVariable UUID projectId,
                                                         @Valid @RequestBody CreateApiKeyRequest request,
                                                         @AuthenticationPrincipal User user) {
        ApiKeyCreatedResponse response = apiKeyService.create(projectId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ApiKeyResponse> list(@PathVariable UUID projectId,
                                     @AuthenticationPrincipal User user) {
        return apiKeyService.listForProject(projectId, user.getId());
    }

    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID projectId,
                                       @PathVariable UUID apiKeyId,
                                       @AuthenticationPrincipal User user) {
        apiKeyService.revoke(apiKeyId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
