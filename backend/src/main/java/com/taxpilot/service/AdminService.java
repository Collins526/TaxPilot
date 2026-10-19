package com.taxpilot.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.audit.AuditLog;
import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.tax.TaxRuleBand;
import com.taxpilot.domain.tax.TaxRuleVersion;
import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.AdminDtos.AuditView;
import com.taxpilot.dto.AdminDtos.BusinessView;
import com.taxpilot.dto.AdminDtos.MembershipRequest;
import com.taxpilot.dto.AdminDtos.MembershipView;
import com.taxpilot.dto.AdminDtos.Overview;
import com.taxpilot.dto.AdminDtos.TaxBandRequest;
import com.taxpilot.dto.AdminDtos.TaxBandView;
import com.taxpilot.dto.AdminDtos.TaxRuleRequest;
import com.taxpilot.dto.AdminDtos.TaxRuleView;
import com.taxpilot.dto.AdminDtos.UserView;
import com.taxpilot.dto.CreateBusinessRequest;
import com.taxpilot.repository.AuditLogRepository;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.BusinessRepository;
import com.taxpilot.repository.FilingPeriodRepository;
import com.taxpilot.repository.TaxRuleBandRepository;
import com.taxpilot.repository.TaxRuleVersionRepository;
import com.taxpilot.repository.UserRepository;

@Service
public class AdminService {

    private static final EnumSet<Role> MEMBERSHIP_ROLES = EnumSet.of(
            Role.BUSINESS_OWNER, Role.BUSINESS_ADMIN, Role.ACCOUNTANT, Role.TAX_ADVISOR, Role.EMPLOYEE);

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final FilingPeriodRepository filingPeriodRepository;
    private final AuditLogRepository auditLogRepository;
    private final TaxRuleVersionRepository taxRuleVersionRepository;
    private final TaxRuleBandRepository taxRuleBandRepository;
    private final AuditService auditService;

    public AdminService(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            BusinessMembershipRepository membershipRepository,
            FilingPeriodRepository filingPeriodRepository,
            AuditLogRepository auditLogRepository,
            TaxRuleVersionRepository taxRuleVersionRepository,
            TaxRuleBandRepository taxRuleBandRepository,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.membershipRepository = membershipRepository;
        this.filingPeriodRepository = filingPeriodRepository;
        this.auditLogRepository = auditLogRepository;
        this.taxRuleVersionRepository = taxRuleVersionRepository;
        this.taxRuleBandRepository = taxRuleBandRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(
                userRepository.count(),
                userRepository.countByEnabledTrue(),
                userRepository.countByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN),
                businessRepository.count(),
                filingPeriodRepository.count());
    }

    @Transactional(readOnly = true)
    public List<UserView> users(int limit) {
        return userRepository.findAllByOrderByEmailAsc(PageRequest.of(0, safeLimit(limit))).stream()
                .map(user -> new UserView(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRoles().stream().map(Enum::name).sorted().toList(),
                        user.isEnabled(),
                        membershipRepository.findByUserId(user.getId()).size()))
                .toList();
    }

    @Transactional
    public UserView setUserEnabled(UUID userId, boolean enabled, UUID actorId) {
        User user = requireUser(userId);
        if (user.isEnabled() == enabled) {
            return toUserView(user);
        }
        if (!enabled && userId.equals(actorId)) {
            throw new IllegalStateException("You cannot disable your own administrator account.");
        }
        if (!enabled && isAdmin(user)
                && userRepository.countByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN) <= 1) {
            throw new IllegalStateException("The last system administrator cannot be disabled.");
        }
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        auditService.record(AuditAction.ADMIN_USER_ENABLED_CHANGED, "USER", userId, null,
                "System administrator " + (enabled ? "enabled" : "disabled") + " account " + user.getEmail());
        return toUserView(saved);
    }

    @Transactional
    public void deleteUser(UUID userId, UUID actorId) {
        User user = requireUser(userId);
        if (userId.equals(actorId)) {
            throw new IllegalStateException("You cannot delete your own administrator account.");
        }
        if (isAdmin(user)
                && userRepository.countByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN) <= 1) {
            throw new IllegalStateException("The last system administrator cannot be deleted.");
        }
        boolean soleBusinessOwner = membershipRepository.findByUserId(userId).stream()
                .filter(BusinessMembership::isActive)
                .filter(membership -> membership.getRole() == Role.BUSINESS_OWNER)
                .anyMatch(membership -> membershipRepository.findByBusinessId(membership.getBusiness().getId()).stream()
                        .filter(BusinessMembership::isActive)
                        .filter(other -> other.getRole() == Role.BUSINESS_OWNER)
                        .count() <= 1);
        if (soleBusinessOwner) {
            throw new IllegalStateException(
                    "This user is the only active owner of a business. Reassign business ownership before deleting the account.");
        }
        auditService.record(AuditAction.ADMIN_USER_DELETED, "USER", userId, null,
                "System administrator deleted account " + user.getEmail());
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<BusinessView> businesses(int limit) {
        return businessRepository.findAllByOrderByNameAsc(PageRequest.of(0, safeLimit(limit))).stream()
                .map(this::toBusinessView)
                .toList();
    }

    @Transactional
    public BusinessView updateBusiness(UUID businessId, CreateBusinessRequest request) {
        Business business = requireBusiness(businessId);
        if (!business.getRegistrationNumber().equalsIgnoreCase(request.registrationNumber())
                && businessRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new IllegalArgumentException("Business registration number already exists");
        }
        applyBusinessUpdate(business, request);
        Business saved = businessRepository.save(business);
        auditService.record(AuditAction.ADMIN_BUSINESS_UPDATED, "BUSINESS", businessId, businessId,
                "System administrator updated business profile " + saved.getName());
        return toBusinessView(saved);
    }

    @Transactional
    public void deleteBusiness(UUID businessId) {
        Business business = requireBusiness(businessId);
        String name = business.getName();
        auditService.record(AuditAction.ADMIN_BUSINESS_DELETED, "BUSINESS", businessId, businessId,
                "System administrator permanently deleted business " + name);
        businessRepository.delete(business);
    }

    @Transactional
    public List<MembershipView> addMembership(UUID businessId, MembershipRequest request) {
        if (request.role() == null || !MEMBERSHIP_ROLES.contains(request.role())) {
            throw new IllegalArgumentException("Choose a supported business membership role.");
        }
        Business business = requireBusiness(businessId);
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("No TaxPilot account exists with that email."));
        if (membershipRepository.existsByBusinessIdAndUserId(businessId, user.getId())) {
            throw new IllegalArgumentException("That user already has a membership for this business.");
        }
        membershipRepository.save(BusinessMembership.builder()
                .business(business)
                .user(user)
                .role(request.role())
                .active(true)
                .build());
        auditService.record(AuditAction.ADMIN_MEMBERSHIP_ADDED, "BUSINESS_MEMBERSHIP",
                user.getId(), businessId, "System administrator added " + user.getEmail()
                        + " as " + request.role() + " to " + business.getName());
        return toBusinessView(business).members();
    }

    @Transactional
    public List<MembershipView> removeMembership(UUID businessId, UUID userId) {
        Business business = requireBusiness(businessId);
        BusinessMembership membership = membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Business membership not found."));
        if (membership.getRole() == Role.BUSINESS_OWNER
                && membershipRepository.findByBusinessId(businessId).stream()
                        .filter(BusinessMembership::isActive)
                        .filter(member -> member.getRole() == Role.BUSINESS_OWNER)
                        .count() <= 1) {
            throw new IllegalStateException("A business must retain at least one active owner.");
        }
        String memberEmail = membership.getUser().getEmail();
        membershipRepository.delete(membership);
        auditService.record(AuditAction.ADMIN_MEMBERSHIP_REMOVED, "BUSINESS_MEMBERSHIP", userId, businessId,
                "System administrator removed " + memberEmail + " from " + business.getName());
        return toBusinessView(business).members();
    }

    @Transactional(readOnly = true)
    public List<AuditView> auditLogs(int limit) {
        return auditLogRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, safeLimit(limit)))
                .map(this::toAuditView)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<TaxRuleView> taxRules() {
        return taxRuleVersionRepository.findAllByOrderByRuleCodeAscEffectiveFromDesc().stream()
                .map(this::toTaxRuleView)
                .toList();
    }

    @Transactional
    public TaxRuleView createTaxRule(TaxRuleRequest request) {
        validateTaxRule(request);
        if (taxRuleVersionRepository.findByRuleCodeAndRuleVersion(request.ruleCode(), request.ruleVersion()).isPresent()) {
            throw new IllegalArgumentException("That rule code and version already exist.");
        }
        TaxRuleVersion version = new TaxRuleVersion();
        applyTaxRule(version, request);
        taxRuleVersionRepository.save(version);
        replaceBands(request.ruleCode(), request.ruleVersion(), request.bands());
        auditService.record(AuditAction.ADMIN_TAX_RULE_CREATED, "TAX_RULE",
                request.ruleCode() + ":" + request.ruleVersion(), null,
                "System administrator created tax rule version " + request.ruleCode() + " " + request.ruleVersion());
        return toTaxRuleView(version);
    }

    @Transactional
    public TaxRuleView updateTaxRule(UUID ruleId, TaxRuleRequest request) {
        validateTaxRule(request);
        TaxRuleVersion version = taxRuleVersionRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Tax rule version not found."));
        if (!version.getRuleCode().equals(request.ruleCode()) || !version.getRuleVersion().equals(request.ruleVersion())) {
            throw new IllegalArgumentException("Rule code and version are immutable; create a new version instead.");
        }
        applyTaxRule(version, request);
        taxRuleVersionRepository.save(version);
        replaceBands(request.ruleCode(), request.ruleVersion(), request.bands());
        auditService.record(AuditAction.ADMIN_TAX_RULE_UPDATED, "TAX_RULE",
                request.ruleCode() + ":" + request.ruleVersion(), null,
                "System administrator updated tax rule version " + request.ruleCode() + " " + request.ruleVersion());
        return toTaxRuleView(version);
    }

    private void replaceBands(String ruleCode, String ruleVersion, List<TaxBandRequest> bands) {
        taxRuleBandRepository.deleteByRuleCodeAndRuleVersion(ruleCode, ruleVersion);
        taxRuleBandRepository.flush();
        for (TaxBandRequest band : bands) {
            TaxRuleBand entity = new TaxRuleBand();
            entity.setRuleCode(ruleCode);
            entity.setRuleVersion(ruleVersion);
            entity.setBandOrder(band.bandOrder());
            entity.setLowerBound(band.lowerBound());
            entity.setUpperBound(band.upperBound());
            entity.setRate(band.rate());
            taxRuleBandRepository.save(entity);
        }
    }

    private void validateTaxRule(TaxRuleRequest request) {
        if (request.ruleCode() == null || request.ruleCode().isBlank()
                || request.ruleVersion() == null || request.ruleVersion().isBlank()
                || request.frequency() == null || request.frequency().isBlank()
                || request.source() == null || request.source().isBlank()
                || request.effectiveFrom() == null
                || request.bands() == null) {
            throw new IllegalArgumentException("Rule code, version, frequency, effective date, source, and bands are required.");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new IllegalArgumentException("Rule effective end date cannot be before its start date.");
        }
        if (request.dueDay() != null && (request.dueDay() < 1 || request.dueDay() > 31)) {
            throw new IllegalArgumentException("Due day must be between 1 and 31.");
        }
        int expectedOrder = 1;
        BigDecimal previousUpperBound = null;
        boolean previousUnbounded = false;
        for (TaxBandRequest band : request.bands().stream()
                .sorted(java.util.Comparator.comparingInt(TaxBandRequest::bandOrder)).toList()) {
            if (band.bandOrder() != expectedOrder++
                    || band.lowerBound() == null
                    || band.lowerBound().signum() < 0
                    || band.rate() == null
                    || band.rate().compareTo(BigDecimal.ZERO) < 0
                    || band.rate().compareTo(BigDecimal.ONE) > 0
                    || (band.upperBound() != null && band.upperBound().compareTo(band.lowerBound()) <= 0)
                    || previousUnbounded
                    || (previousUpperBound != null && previousUpperBound.compareTo(band.lowerBound()) > 0)) {
                throw new IllegalArgumentException("Tax bands must be ordered, non-overlapping, and have valid bounds and rates.");
            }
            previousUnbounded = band.upperBound() == null;
            previousUpperBound = band.upperBound();
        }
    }

    private void applyTaxRule(TaxRuleVersion entity, TaxRuleRequest request) {
        entity.setRuleCode(request.ruleCode().trim());
        entity.setRuleVersion(request.ruleVersion().trim());
        entity.setFrequency(request.frequency().trim());
        entity.setDueDay(request.dueDay());
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveTo(request.effectiveTo());
        entity.setRate(request.rate());
        entity.setThreshold(request.threshold());
        entity.setSource(request.source().trim());
    }

    private TaxRuleView toTaxRuleView(TaxRuleVersion version) {
        List<TaxBandView> bands = taxRuleBandRepository
                .findByRuleCodeAndRuleVersionOrderByBandOrderAsc(version.getRuleCode(), version.getRuleVersion())
                .stream()
                .map(band -> new TaxBandView(
                        band.getBandOrder(), band.getLowerBound(), band.getUpperBound(), band.getRate()))
                .toList();
        return new TaxRuleView(
                version.getId(),
                version.getRuleCode(),
                version.getRuleVersion(),
                version.getFrequency(),
                version.getDueDay(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                version.getRate(),
                version.getThreshold(),
                version.getSource(),
                bands);
    }

    private UserView toUserView(User user) {
        return new UserView(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getRoles().stream().map(Enum::name).sorted().toList(), user.isEnabled(),
                membershipRepository.findByUserId(user.getId()).size());
    }

    private BusinessView toBusinessView(Business business) {
        List<MembershipView> members = membershipRepository.findByBusinessId(business.getId()).stream()
                .map(member -> new MembershipView(
                        member.getUser().getId(),
                        (member.getUser().getFirstName() + " " + member.getUser().getLastName()).trim(),
                        member.getUser().getEmail(),
                        member.getRole(),
                        member.isActive()))
                .toList();
        return new BusinessView(
                business.getId(),
                business.getName(),
                business.getLegalName(),
                business.getRegistrationNumber(),
                business.getKraPin(),
                business.getIndustry(),
                business.getBusinessType(),
                business.getAnnualTurnover(),
                business.getEmployeeCount(),
                business.isVatRegistered(),
                business.isEtimsRegistered(),
                business.getCountryCode(),
                business.getTimezone(),
                members);
    }

    private AuditView toAuditView(AuditLog entry) {
        return new AuditView(
                entry.getId(),
                entry.getOccurredAt(),
                entry.getActorUserId(),
                entry.getActorEmail(),
                entry.getAction(),
                entry.getResourceType(),
                entry.getResourceId(),
                entry.getBusinessId(),
                entry.getOutcome(),
                entry.getDetails());
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private Business requireBusiness(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Business not found."));
    }

    private void applyBusinessUpdate(Business business, CreateBusinessRequest request) {
        business.setName(request.name());
        business.setLegalName(request.legalName());
        business.setRegistrationNumber(request.registrationNumber());
        business.setKraPin(request.kraPin());
        business.setIndustry(request.industry());
        business.setBusinessType(request.businessType());
        business.setAnnualTurnover(request.annualTurnover());
        business.setEmployeeCount(request.employeeCount());
        business.setVatRegistered(Boolean.TRUE.equals(request.vatRegistered()));
        business.setEtimsRegistered(Boolean.TRUE.equals(request.etimsRegistered()));
        business.setCountryCode(request.countryCode() == null || request.countryCode().isBlank()
                ? "KE" : request.countryCode());
        business.setTimezone(request.timezone() == null || request.timezone().isBlank()
                ? "Africa/Nairobi" : request.timezone());
    }

    private boolean isAdmin(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.SUPER_ADMIN);
    }

    private int safeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 500);
    }
}
