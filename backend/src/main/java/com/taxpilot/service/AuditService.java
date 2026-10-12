package com.taxpilot.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.audit.AuditLog;
import com.taxpilot.domain.audit.AuditOutcome;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.AuditLogResponse;
import com.taxpilot.repository.AuditLogRepository;

@Service
public class AuditService {

    private static final int MAX_DETAILS_LENGTH = 1024;

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(AuditAction action, String resourceType, Object resourceId, UUID businessId, String details) {
        User actor = currentUser();
        record(actor == null ? null : actor.getId(),
                actor == null ? null : actor.getEmail(),
                action,
                resourceType,
                resourceId,
                businessId,
                AuditOutcome.SUCCESS,
                details);
    }

    @Transactional
    public void record(UUID actorUserId,
                       String actorEmail,
                       AuditAction action,
                       String resourceType,
                       Object resourceId,
                       UUID businessId,
                       AuditOutcome outcome,
                       String details) {
        AuditLog entry = new AuditLog();
        entry.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        entry.setActorUserId(actorUserId);
        entry.setActorEmail(actorEmail);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId == null ? null : resourceId.toString());
        entry.setBusinessId(businessId);
        entry.setOutcome(outcome);
        entry.setDetails(truncate(details));
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listForBusiness(UUID businessId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return auditLogRepository.findByBusinessIdOrderByOccurredAtDesc(businessId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getOccurredAt(),
                entry.getActorUserId(),
                entry.getActorEmail(),
                entry.getAction(),
                entry.getResourceType(),
                entry.getResourceId(),
                entry.getBusinessId(),
                entry.getOutcome(),
                entry.getDetails()
        );
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() <= MAX_DETAILS_LENGTH ? details : details.substring(0, MAX_DETAILS_LENGTH);
    }
}
