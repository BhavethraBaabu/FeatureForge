package com.featureforge.service;

import com.featureforge.domain.ApiKey;
import com.featureforge.domain.OrgRole;
import com.featureforge.domain.Project;
import com.featureforge.dto.ApiKeyCreatedResponse;
import com.featureforge.dto.CreateApiKeyRequest;
import com.featureforge.exception.InvalidApiKeyException;
import com.featureforge.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ApiKeyService apiKeyService;

    private UUID projectId;
    private UUID orgId;
    private UUID requesterId;
    private Project project;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository, projectService, accessControlService, passwordEncoder);

        projectId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        requesterId = UUID.randomUUID();

        project = Project.builder()
                .id(projectId)
                .organizationId(orgId)
                .build();
    }

    @Test
    void create_generatesKeyWithCorrectPrefixTag_andHashesBeforeStoring() {
        when(projectService.findByIdOrThrow(projectId)).thenReturn(project);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-value");

        CreateApiKeyRequest request = new CreateApiKeyRequest("CI pipeline key");

        ApiKeyCreatedResponse response = apiKeyService.create(projectId, request, requesterId);

        verify(accessControlService).requireRole(orgId, requesterId, OrgRole.ADMIN);

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey saved = captor.getValue();

        assertThat(saved.getHashedKey()).isEqualTo("hashed-value");
        assertThat(saved.getKeyPrefix()).startsWith("ffsdk_");
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getCreatedBy()).isEqualTo(requesterId);

        assertThat(response.rawKey()).startsWith("ffsdk_");
        assertThat(response.rawKey()).isNotEqualTo(saved.getHashedKey());
    }

    @Test
    void resolveProjectId_nullKey_throwsInvalidApiKeyException() {
        assertThatThrownBy(() -> apiKeyService.resolveProjectId(null))
                .isInstanceOf(InvalidApiKeyException.class);

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void resolveProjectId_keyShorterThanPrefixLength_throwsInvalidApiKeyException() {
        assertThatThrownBy(() -> apiKeyService.resolveProjectId("short"))
                .isInstanceOf(InvalidApiKeyException.class);

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void resolveProjectId_unknownPrefix_throwsInvalidApiKeyException() {
        String rawKey = "ffsdk_" + "a".repeat(64);
        String prefix = rawKey.substring(0, 12);

        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.resolveProjectId(rawKey))
                .isInstanceOf(InvalidApiKeyException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void resolveProjectId_revokedKey_throwsInvalidApiKeyException_withoutCheckingHash() {
        String rawKey = "ffsdk_" + "a".repeat(64);
        String prefix = rawKey.substring(0, 12);

        ApiKey revoked = ApiKey.builder()
                .projectId(projectId)
                .keyPrefix(prefix)
                .hashedKey("hashed-value")
                .revoked(true)
                .build();

        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> apiKeyService.resolveProjectId(rawKey))
                .isInstanceOf(InvalidApiKeyException.class)
                .hasMessageContaining("revoked");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void resolveProjectId_wrongSecret_throwsInvalidApiKeyException() {
        String rawKey = "ffsdk_" + "a".repeat(64);
        String prefix = rawKey.substring(0, 12);

        ApiKey stored = ApiKey.builder()
                .projectId(projectId)
                .keyPrefix(prefix)
                .hashedKey("hashed-value")
                .revoked(false)
                .build();

        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches(rawKey, "hashed-value")).thenReturn(false);

        assertThatThrownBy(() -> apiKeyService.resolveProjectId(rawKey))
                .isInstanceOf(InvalidApiKeyException.class);

        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void resolveProjectId_validKey_returnsProjectId_andUpdatesLastUsedAt() {
        String rawKey = "ffsdk_" + "a".repeat(64);
        String prefix = rawKey.substring(0, 12);

        ApiKey stored = ApiKey.builder()
                .projectId(projectId)
                .keyPrefix(prefix)
                .hashedKey("hashed-value")
                .revoked(false)
                .build();

        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches(rawKey, "hashed-value")).thenReturn(true);

        UUID resolved = apiKeyService.resolveProjectId(rawKey);

        assertThat(resolved).isEqualTo(projectId);
        assertThat(stored.getLastUsedAt()).isNotNull();
        verify(apiKeyRepository).save(stored);
    }

    @Test
    void revoke_setsRevokedFlag_afterAccessCheck() {
        UUID apiKeyId = UUID.randomUUID();
        ApiKey stored = ApiKey.builder()
                .id(apiKeyId)
                .projectId(projectId)
                .revoked(false)
                .build();

        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(stored));
        when(projectService.findByIdOrThrow(projectId)).thenReturn(project);

        apiKeyService.revoke(apiKeyId, requesterId);

        verify(accessControlService).requireRole(orgId, requesterId, OrgRole.ADMIN);
        assertThat(stored.isRevoked()).isTrue();
        verify(apiKeyRepository).save(stored);
    }
}
