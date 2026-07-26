package com.featureforge.service;

import com.featureforge.domain.OrgRole;
import com.featureforge.domain.OrganizationMember;
import com.featureforge.exception.ForbiddenOperationException;
import com.featureforge.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationMember requireMembership(UUID organizationId, UUID userId) {
        return organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "You are not a member of this organization"));
    }

    public void requireRole(UUID organizationId, UUID userId, OrgRole minimumRole) {
        OrganizationMember member = requireMembership(organizationId, userId);
        if (!member.getRole().atLeast(minimumRole)) {
            throw new ForbiddenOperationException(
                    "This action requires %s role or higher".formatted(minimumRole));
        }
    }
}
