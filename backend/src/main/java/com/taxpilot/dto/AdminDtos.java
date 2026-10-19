package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.audit.AuditOutcome;
import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.domain.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record Overview(long users, long enabledUsers, long admins, long businesses, long filingPeriods) {
    }

    public record UserView(
            UUID id,
            String firstName,
            String lastName,
            String email,
            List<String> roles,
            boolean enabled,
            int businessCount) {
    }

    public record MembershipView(UUID userId, String name, String email, Role role, boolean active) {
    }

    public record BusinessView(
            UUID id,
            String name,
            String legalName,
            String registrationNumber,
            String kraPin,
            String industry,
            BusinessType businessType,
            BigDecimal annualTurnover,
            Integer employeeCount,
            boolean vatRegistered,
            boolean etimsRegistered,
            String countryCode,
            String timezone,
            List<MembershipView> members) {
    }

    public record AuditView(
            UUID id,
            LocalDateTime occurredAt,
            UUID actorUserId,
            String actorEmail,
            AuditAction action,
            String resourceType,
            String resourceId,
            UUID businessId,
            AuditOutcome outcome,
            String details) {
    }

    public record TaxBandView(int bandOrder, BigDecimal lowerBound, BigDecimal upperBound, BigDecimal rate) {
    }

    public record TaxRuleView(
            UUID id,
            String ruleCode,
            String ruleVersion,
            String frequency,
            Integer dueDay,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            BigDecimal rate,
            BigDecimal threshold,
            String source,
            List<TaxBandView> bands) {
    }

    public record EnabledRequest(boolean enabled) {
    }

    public record MembershipRequest(@NotBlank String email, @NotNull Role role) {
    }

    public record TaxBandRequest(
            @Min(1)
            int bandOrder,
            @NotNull @DecimalMin("0.00")
            BigDecimal lowerBound,
            @DecimalMin("0.00")
            BigDecimal upperBound,
            @NotNull @DecimalMin("0.00") @jakarta.validation.constraints.DecimalMax("1.00")
            BigDecimal rate) {
    }

    public record TaxRuleRequest(
            @NotBlank String ruleCode,
            @NotBlank String ruleVersion,
            @NotBlank String frequency,
            @Min(1) @Max(31)
            Integer dueDay,
            @NotNull
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            @DecimalMin("0.00") @jakarta.validation.constraints.DecimalMax("1.00")
            BigDecimal rate,
            @DecimalMin("0.00")
            BigDecimal threshold,
            @NotBlank String source,
            @NotNull List<@Valid TaxBandRequest> bands) {
    }
}
