package com.taxpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.AuditLogRepository;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.BusinessRepository;
import com.taxpilot.repository.FilingPeriodRepository;
import com.taxpilot.repository.TaxRuleBandRepository;
import com.taxpilot.repository.TaxRuleVersionRepository;
import com.taxpilot.repository.UserRepository;

class AdminServiceTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final BusinessRepository businessRepository = org.mockito.Mockito.mock(BusinessRepository.class);
    private final BusinessMembershipRepository membershipRepository =
            org.mockito.Mockito.mock(BusinessMembershipRepository.class);
    private final FilingPeriodRepository filingPeriodRepository =
            org.mockito.Mockito.mock(FilingPeriodRepository.class);
    private final AuditLogRepository auditLogRepository = org.mockito.Mockito.mock(AuditLogRepository.class);
    private final TaxRuleVersionRepository taxRuleVersionRepository =
            org.mockito.Mockito.mock(TaxRuleVersionRepository.class);
    private final TaxRuleBandRepository taxRuleBandRepository =
            org.mockito.Mockito.mock(TaxRuleBandRepository.class);
    private final AuditService auditService = org.mockito.Mockito.mock(AuditService.class);
    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(
                userRepository,
                businessRepository,
                membershipRepository,
                filingPeriodRepository,
                auditLogRepository,
                taxRuleVersionRepository,
                taxRuleBandRepository,
                auditService);
    }

    @Test
    void preventsDisablingTheCurrentAdministrator() {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .id(adminId)
                .email("admin@taxpilot.com")
                .roles(EnumSet.of(Role.ADMIN))
                .enabled(true)
                .build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class, () -> service.setUserEnabled(adminId, false, adminId));

        assertEquals(true, admin.isEnabled());
        verify(userRepository, never()).save(admin);
    }

    @Test
    void preventsDisablingOrDeletingTheLastAdmin() {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .id(adminId)
                .email("admin@taxpilot.com")
                .roles(EnumSet.of(Role.ADMIN))
                .enabled(true)
                .build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.setUserEnabled(adminId, false, UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> service.deleteUser(adminId, UUID.randomUUID()));

        verify(userRepository, never()).delete(admin);
    }
}
