package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.BusinessMemberInviteRequest;
import com.taxpilot.dto.BusinessMemberRole;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.UserRepository;
import com.taxpilot.service.AuditService;
import com.taxpilot.service.BusinessMemberService;

class BusinessMemberServiceTest {

    private final BusinessMembershipRepository membershipRepository = Mockito.mock(BusinessMembershipRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AuditService auditService = Mockito.mock(AuditService.class);
    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private BusinessMemberService service;

    @BeforeEach
    void setUp() {
        service = new BusinessMemberService(membershipRepository, userRepository, auditService);
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, ownerId))
                .thenReturn(Optional.of(membership(ownerId, "Owner", "Njeri", "owner@example.com", Role.BUSINESS_OWNER, true)));
    }

    @Test
    void listsMembersForOwnerOnly() {
        Mockito.when(membershipRepository.findByBusinessId(businessId)).thenReturn(List.of(
                membership(ownerId, "Owner", "Njeri", "owner@example.com", Role.BUSINESS_OWNER, true),
                membership(memberId, "Alex", "Accountant", "alex@example.com", Role.ACCOUNTANT, true)));

        var members = service.listMembers(ownerId, businessId);

        assertEquals(2, members.size());
        assertEquals("ACCOUNTANT", members.get(0).role());
        Mockito.verify(membershipRepository).findByBusinessId(businessId);
    }

    @Test
    void addsRegisteredAccountantAndRecordsAudit() {
        User accountant = User.builder()
                .id(memberId)
                .firstName("Alex")
                .lastName("Accountant")
                .email("alex@example.com")
                .build();
        Mockito.when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(accountant));
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, memberId)).thenReturn(Optional.empty());
        Mockito.when(membershipRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var added = service.addMember(ownerId, businessId,
                new BusinessMemberInviteRequest("alex@example.com", BusinessMemberRole.ACCOUNTANT));

        assertEquals(memberId, added.userId());
        assertEquals("ACCOUNTANT", added.role());
        Mockito.verify(auditService).record(eq(com.taxpilot.domain.audit.AuditAction.BUSINESS_MEMBER_ADDED),
                eq("BUSINESS_MEMBERSHIP"), any(), eq(businessId), any());
    }

    @Test
    void rejectsUnregisteredUsersAndDuplicateActiveMembers() {
        Mockito.when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.addMember(ownerId, businessId,
                new BusinessMemberInviteRequest("unknown@example.com", BusinessMemberRole.ACCOUNTANT)));

        User member = User.builder().id(memberId).firstName("Alex").lastName("Accountant")
                .email("alex@example.com").build();
        Mockito.when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(member));
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, memberId))
                .thenReturn(Optional.of(membership(memberId, "Alex", "Accountant", "alex@example.com",
                        Role.ACCOUNTANT, true)));
        assertThrows(ResponseStatusException.class, () -> service.addMember(ownerId, businessId,
                new BusinessMemberInviteRequest("alex@example.com", BusinessMemberRole.ACCOUNTANT)));
    }

    @Test
    void revokesMemberAccessButProtectsOwnerAndSelf() {
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, memberId))
                .thenReturn(Optional.of(membership(memberId, "Alex", "Accountant", "alex@example.com",
                        Role.ACCOUNTANT, true)));

        service.removeMember(ownerId, businessId, memberId);

        Mockito.verify(membershipRepository).findByBusinessIdAndUserId(businessId, ownerId);
        Mockito.verify(auditService).record(eq(com.taxpilot.domain.audit.AuditAction.BUSINESS_MEMBER_REMOVED),
                eq("BUSINESS_MEMBERSHIP"), any(), eq(businessId), any());
        assertThrows(ResponseStatusException.class, () -> service.removeMember(ownerId, businessId, ownerId));
    }

    @Test
    void rejectsNonOwnerTeamManagement() {
        UUID accountantId = UUID.randomUUID();
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, accountantId))
                .thenReturn(Optional.of(membership(accountantId, "Alex", "Accountant", "alex@example.com",
                        Role.ACCOUNTANT, true)));

        assertThrows(ResponseStatusException.class, () -> service.listMembers(accountantId, businessId));
    }

    private BusinessMembership membership(UUID userId, String firstName, String lastName, String email,
                                          Role role, boolean active) {
        Business business = Business.builder()
                .id(businessId)
                .name("Njeri Traders")
                .legalName("Jane Njeri")
                .registrationNumber("PVT-1001")
                .kraPin("A123456789Z")
                .industry("Retail")
                .businessType(BusinessType.SOLE_PROPRIETOR)
                .countryCode("KE")
                .timezone("Africa/Nairobi")
                .build();
        User user = User.builder().id(userId).firstName(firstName).lastName(lastName).email(email).build();
        return BusinessMembership.builder()
                .business(business)
                .user(user)
                .role(role)
                .active(active)
                .build();
    }
}
