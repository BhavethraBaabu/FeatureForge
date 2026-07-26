package com.featureforge.service;

import com.featureforge.domain.OrgRole;
import com.featureforge.domain.Organization;
import com.featureforge.domain.OrganizationMember;
import com.featureforge.dto.AddOrganizationMemberRequest;
import com.featureforge.dto.CreateOrganizationRequest;
import com.featureforge.dto.OrganizationResponse;
import com.featureforge.exception.DuplicateResourceException;
import com.featureforge.exception.ResourceNotFoundException;
import com.featureforge.repository.OrganizationMemberRepository;
import com.featureforge.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AccessControlService accessControlService;

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, UUID creatorUserId) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("An organization with slug '%s' already exists".formatted(request.slug()));
        }

        Organization org = Organization.builder()
                .name(request.name())
                .slug(request.slug())
                .build();
        organizationRepository.save(org);

        OrganizationMember owner = OrganizationMember.builder()
                .organizationId(org.getId())
                .userId(creatorUserId)
                .role(OrgRole.OWNER)
                .build();
        organizationMemberRepository.save(owner);

        log.info("Organization '{}' created by user {}", org.getSlug(), creatorUserId);
        return OrganizationResponse.fromEntity(org, OrgRole.OWNER);
    }

    public List<OrganizationResponse> listForUser(UUID userId) {
        List<OrganizationMember> memberships = organizationMemberRepository.findByUserId(userId);

        return memberships.stream()
                .map(m -> {
                    Organization org = organizationRepository.findById(m.getOrganizationId())
                            .orElseThrow(() -> new ResourceNotFoundException("Organization", m.getOrganizationId()));
                    return OrganizationResponse.fromEntity(org, m.getRole());
                })
                .toList();
    }

    public OrganizationResponse get(UUID organizationId, UUID requesterId) {
        OrganizationMember member = accessControlService.requireMembership(organizationId, requesterId);
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        return OrganizationResponse.fromEntity(org, member.getRole());
    }

    @Transactional
    public void addMember(UUID organizationId, AddOrganizationMemberRequest request, UUID requesterId) {
        accessControlService.requireRole(organizationId, requesterId, OrgRole.ADMIN);

        if (organizationMemberRepository.existsByOrganizationIdAndUserId(organizationId, request.userId())) {
            throw new DuplicateResourceException("User is already a member of this organization");
        }

        OrganizationMember member = OrganizationMember.builder()
                .organizationId(organizationId)
                .userId(request.userId())
                .role(request.role())
                .build();
        organizationMemberRepository.save(member);
        log.info("User {} added to organization {} as {}", request.userId(), organizationId, request.role());
    }
}
