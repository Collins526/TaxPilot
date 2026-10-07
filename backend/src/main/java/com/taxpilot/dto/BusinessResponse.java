package com.taxpilot.dto;

import java.util.List;
import java.util.UUID;

import com.taxpilot.domain.business.BusinessType;

public record BusinessResponse(
        UUID id,
        String name,
        String legalName,
        String registrationNumber,
        String kraPin,
        String industry,
        BusinessType businessType,
        java.math.BigDecimal annualTurnover,
        Integer employeeCount,
        boolean vatRegistered,
        boolean etimsRegistered,
        String countryCode,
        String timezone,
        List<String> memberships
) {
}
