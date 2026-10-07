package com.taxpilot.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.business.TaxProfile;

public interface TaxProfileRepository extends JpaRepository<TaxProfile, UUID> {
    Optional<TaxProfile> findByBusinessId(UUID businessId);
}
