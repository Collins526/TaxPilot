package com.taxpilot.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.audit.AuditOutcome;

public record AuditLogResponse(
        UUID id,
        LocalDateTime occurredAt,
        UUID actorUserId,
        String actorEmail,
        AuditAction action,
        String resourceType,
        String resourceId,
        UUID businessId,
        AuditOutcome outcome,
        String details
) {
}
