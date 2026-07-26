package com.featureforge.controller;

import com.featureforge.domain.User;
import com.featureforge.dto.AddOrganizationMemberRequest;
import com.featureforge.dto.CreateOrganizationRequest;
import com.featureforge.dto.OrganizationResponse;
import com.featureforge.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request,
                                                        @AuthenticationPrincipal User user) {
        OrganizationResponse response = organizationService.create(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<OrganizationResponse> listMine(@AuthenticationPrincipal User user) {
        return organizationService.listForUser(user.getId());
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@PathVariable UUID organizationId,
                                    @AuthenticationPrincipal User user) {
        return organizationService.get(organizationId, user.getId());
    }

    @PostMapping("/{organizationId}/members")
    public ResponseEntity<Void> addMember(@PathVariable UUID organizationId,
                                          @Valid @RequestBody AddOrganizationMemberRequest request,
                                          @AuthenticationPrincipal User user) {
        organizationService.addMember(organizationId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
