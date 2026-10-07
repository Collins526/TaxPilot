package com.taxpilot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.business.BusinessMembership;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, UUID> {
    Optional<BusinessMembership> findByBusinessIdAndUserId(UUID businessId, UUID userId);

    List<BusinessMembership> findByUserId(UUID userId);

    boolean existsByBusinessIdAndUserId(UUID businessId, UUID userId);
}
