package com.taxpilot.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.business.Business;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
    boolean existsByRegistrationNumber(String registrationNumber);
}
