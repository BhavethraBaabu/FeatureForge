package com.featureforge.service;

import com.featureforge.domain.ApiKey;
import com.featureforge.domain.OrgRole;
import com.featureforge.domain.Project;
import com.featureforge.dto.ApiKeyCreatedResponse;
import com.featureforge.dto.ApiKeyResponse;
import com.featureforge.dto.CreateApiKeyRequest;
import com.featureforge.exception.InvalidApiKeyException;
import com.featureforge.exception.ResourceNotFoundException;
import com.featureforge.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SDK keys are validated on every evaluation call, so lookup has to be O(1)
 * on an index — not a linear BCrypt scan over every key in the system.
 * `keyPrefix` (unique, indexed) narrows to a single row; BCrypt only runs
 * once, against that row's hash, to confirm the full raw key matches.
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String KEY_PREFIX_TAG = "ffsdk_";
    private static final int PREFIX_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectService projectService;
    private final AccessControlService accessControlService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApiKeyCreatedResponse create(UUID projectId, CreateApiKeyRequest request, UUID requesterId) {
        Project project = projectService.findByIdOrThrow(projectId);
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        String rawKey = generateRawKey();
        String keyPrefix = rawKey.substring(0, PREFIX_LENGTH);

        ApiKey apiKey = ApiKey.builder()
                .projectId(projectId)
                .name(request.name())
                .keyPrefix(keyPrefix)
                .hashedKey(passwordEncoder.encode(rawKey))
                .createdBy(requesterId)
                .build();
        apiKeyRepository.save(apiKey);

        log.info("API key '{}' created for project {} by user {}", request.name(), projectId, requesterId);
        return new ApiKeyCreatedResponse(apiKey.getId(), apiKey.getName(), keyPrefix, rawKey, apiKey.getCreatedAt());
    }

    public List<ApiKeyResponse> listForProject(UUID projectId, UUID requesterId) {
        Project project = projectService.findByIdOrThrow(projectId);
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        return apiKeyRepository.findByProjectId(projectId).stream()
                .map(ApiKeyResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void revoke(UUID apiKeyId, UUID requesterId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key", apiKeyId));
        Project project = projectService.findByIdOrThrow(apiKey.getProjectId());
        accessControlService.requireRole(project.getOrganizationId(), requesterId, OrgRole.ADMIN);

        apiKey.setRevoked(true);
        apiKeyRepository.save(apiKey);
        log.info("API key {} revoked by user {}", apiKeyId, requesterId);
    }

    /**
     * Resolves a raw SDK key (from the X-API-Key header) to the project it
     * belongs to. Called on every SDK request, so failure paths are cheap —
     * only a successful prefix match pays the BCrypt cost.
     */
    @Transactional
    public UUID resolveProjectId(String rawKey) {
        if (rawKey == null || rawKey.length() <= PREFIX_LENGTH) {
            throw new InvalidApiKeyException("Missing or malformed API key");
        }

        String prefix = rawKey.substring(0, PREFIX_LENGTH);
        ApiKey apiKey = apiKeyRepository.findByKeyPrefix(prefix)
                .orElseThrow(() -> new InvalidApiKeyException("Unknown API key"));

        if (apiKey.isRevoked()) {
            throw new InvalidApiKeyException("API key has been revoked");
        }

        if (!passwordEncoder.matches(rawKey, apiKey.getHashedKey())) {
            throw new InvalidApiKeyException("Invalid API key");
        }

        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        return apiKey.getProjectId();
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return KEY_PREFIX_TAG + hex;
    }
}
