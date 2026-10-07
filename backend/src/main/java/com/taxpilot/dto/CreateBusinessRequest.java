package com.taxpilot.dto;

import com.taxpilot.domain.business.BusinessType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record CreateBusinessRequest(
        @NotBlank(message = "Business name is required") String name,
        @NotBlank(message = "Legal name is required") String legalName,
        @NotBlank(message = "Registration number is required") String registrationNumber,
        @NotBlank(message = "KRA PIN is required") String kraPin,
        @NotBlank(message = "Industry is required") String industry,
        @NotNull(message = "Business type is required") BusinessType businessType,
        @DecimalMin(value = "0.00", message = "Annual turnover must be zero or greater") java.math.BigDecimal annualTurnover,
        @Min(value = 0, message = "Employee count must be zero or greater") Integer employeeCount,
        Boolean vatRegistered,
        Boolean etimsRegistered,
        String countryCode,
        String timezone
) {
}
