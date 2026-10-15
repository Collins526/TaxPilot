package com.taxpilot.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.tax.FilingDeadlineStatus;

public interface FilingDeadlineStatusRepository extends JpaRepository<FilingDeadlineStatus, UUID> {
    Optional<FilingDeadlineStatus> findByBusinessIdAndTaxTypeAndDueDate(
            UUID businessId, String taxType, LocalDate dueDate);
}
