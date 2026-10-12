package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.audit.AuditLog;
import com.taxpilot.domain.audit.AuditOutcome;
import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.AuditLogResponse;
import com.taxpilot.repository.AuditLogRepository;
import com.taxpilot.service.AuditService;

class AuditServiceTest {

    private final AuditLogRepository auditLogRepository = Mockito.mock(AuditLogRepository.class);
    private final AuditService auditService = new AuditService(auditLogRepository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsActorFromSecurityContext() {
        User actor = user("owner@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));

        UUID businessId = UUID.randomUUID();
        auditService.record(AuditAction.BUSINESS_CREATED, "BUSINESS", businessId, businessId, "created");

        AuditLog saved = captureSingleSave();
        assertEquals(actor.getId(), saved.getActorUserId());
        assertEquals("owner@example.com", saved.getActorEmail());
        assertEquals(AuditAction.BUSINESS_CREATED, saved.getAction());
        assertEquals(AuditOutcome.SUCCESS, saved.getOutcome());
        assertEquals(businessId, saved.getBusinessId());
        assertEquals(businessId.toString(), saved.getResourceId());
        assertNotNull(saved.getOccurredAt());
    }

    @Test
    void recordsExplicitFailureWithoutAuthenticatedActor() {
        auditService.record(null, "intruder@example.com", AuditAction.LOGIN_FAILED, "USER",
                null, null, AuditOutcome.FAILURE, "Invalid credentials");

        AuditLog saved = captureSingleSave();
        assertNull(saved.getActorUserId());
        assertEquals("intruder@example.com", saved.getActorEmail());
        assertEquals(AuditOutcome.FAILURE, saved.getOutcome());
    }

    @Test
    void truncatesLongDetails() {
        String longDetails = "x".repeat(2000);
        auditService.record(AuditAction.BUSINESS_UPDATED, "BUSINESS", UUID.randomUUID(), null, longDetails);

        AuditLog saved = captureSingleSave();
        assertEquals(1024, saved.getDetails().length());
    }

    @Test
    void listsBusinessEntriesWithBoundsApplied() {
        UUID businessId = UUID.randomUUID();
        AuditLog entry = new AuditLog();
        entry.setAction(AuditAction.BUSINESS_UPDATED);
        entry.setResourceType("BUSINESS");
        entry.setOutcome(AuditOutcome.SUCCESS);
        Mockito.when(auditLogRepository.findByBusinessIdOrderByOccurredAtDesc(Mockito.eq(businessId), Mockito.any()))
                .thenReturn(List.of(entry));

        List<AuditLogResponse> result = auditService.listForBusiness(businessId, 0);

        assertEquals(1, result.size());
        assertEquals(AuditAction.BUSINESS_UPDATED, result.get(0).action());
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        Mockito.verify(auditLogRepository)
                .findByBusinessIdOrderByOccurredAtDesc(Mockito.eq(businessId), pageable.capture());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    private AuditLog captureSingleSave() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        Mockito.verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("Test")
                .lastName("Owner")
                .email(email)
                .phone("+254700000000")
                .passwordHash("hash")
                .roles(java.util.EnumSet.of(Role.BUSINESS_OWNER))
                .enabled(true)
                .build();
    }
}
