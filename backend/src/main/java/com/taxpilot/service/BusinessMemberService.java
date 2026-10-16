package com.taxpilot.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.BusinessMemberInviteRequest;
import com.taxpilot.dto.BusinessMemberRole;
import com.taxpilot.dto.BusinessMemberResponse;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.UserRepository;

@Service
public class BusinessMemberService {

    private final BusinessMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BusinessMemberService(BusinessMembershipRepository membershipRepository,
                                 UserRepository userRepository,
                                 AuditService auditService) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BusinessMemberResponse> listMembers(UUID actorUserId, UUID businessId) {
        requireOwner(actorUserId, businessId);
        return membershipRepository.findByBusinessId(businessId).stream()
                .filter(BusinessMembership::isActive)
                .sorted(Comparator.comparing(member -> member.getUser().getFirstName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BusinessMemberResponse addMember(UUID actorUserId, UUID businessId, BusinessMemberInviteRequest request) {
        BusinessMembership owner = requireOwner(actorUserId, businessId);
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No TaxPilot account exists for that email address"));
        if (user.getId().equals(actorUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already own this business");
        }

        BusinessMembership membership = membershipRepository.findByBusinessIdAndUserId(businessId, user.getId())
                .orElseGet(() -> BusinessMembership.builder()
                        .business(owner.getBusiness())
                        .user(user)
                        .build());
        if (membership.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user already has access to the business");
        }

        membership.setRole(toDomainRole(request.role()));
        membership.setActive(true);
        BusinessMembership saved = membershipRepository.save(membership);
        auditService.record(AuditAction.BUSINESS_MEMBER_ADDED, "BUSINESS_MEMBERSHIP",
                saved.getId(), businessId, "Business member added with role " + saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    public void removeMember(UUID actorUserId, UUID businessId, UUID memberUserId) {
        requireOwner(actorUserId, businessId);
        if (actorUserId.equals(memberUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The business owner cannot remove their own access");
        }
        BusinessMembership membership = membershipRepository.findByBusinessIdAndUserId(businessId, memberUserId)
                .filter(BusinessMembership::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business member not found"));
        if (membership.getRole() == Role.BUSINESS_OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business owner access cannot be removed");
        }

        membership.setActive(false);
        auditService.record(AuditAction.BUSINESS_MEMBER_REMOVED, "BUSINESS_MEMBERSHIP",
                membership.getId(), businessId, "Business member access removed");
    }

    private BusinessMembership requireOwner(UUID userId, UUID businessId) {
        return membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .filter(BusinessMembership::isActive)
                .filter(membership -> membership.getRole() == Role.BUSINESS_OWNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
    }

    private Role toDomainRole(BusinessMemberRole role) {
        return switch (role) {
            case ACCOUNTANT -> Role.ACCOUNTANT;
            case TAX_ADVISOR -> Role.TAX_ADVISOR;
            case EMPLOYEE -> Role.EMPLOYEE;
        };
    }

    private BusinessMemberResponse toResponse(BusinessMembership membership) {
        User user = membership.getUser();
        return new BusinessMemberResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), membership.getRole().name());
    }
}
