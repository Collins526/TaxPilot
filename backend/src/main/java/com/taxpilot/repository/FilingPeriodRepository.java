package com.taxpilot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.tax.FilingPeriod;

public interface FilingPeriodRepository extends JpaRepository<FilingPeriod, UUID> {
    List<FilingPeriod> findByBusinessIdOrderByPeriodEndDesc(UUID businessId);

    Optional<FilingPeriod> findByIdAndBusinessId(UUID id, UUID businessId);

    Optional<FilingPeriod> findFirstByBusinessIdOrderByPeriodEndDesc(UUID businessId);

    boolean existsByBusinessIdAndPeriodStartAndPeriodEnd(UUID businessId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd);
}
