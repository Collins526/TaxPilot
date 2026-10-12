package com.taxpilot.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.audit.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByBusinessIdOrderByOccurredAtDesc(UUID businessId, Pageable pageable);
}
