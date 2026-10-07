package com.taxpilot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.business.TaxProfile;
import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.BusinessResponse;
import com.taxpilot.dto.CreateBusinessRequest;
import com.taxpilot.dto.DashboardSummaryResponse;
import com.taxpilot.dto.TaxCalendarEntry;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.BusinessRepository;
import com.taxpilot.repository.TaxProfileRepository;
import com.taxpilot.repository.UserRepository;
import com.taxpilot.tax.TaxHealthService;
import com.taxpilot.tax.TaxHealthScore;
import com.taxpilot.tax.TaxObligation;
import com.taxpilot.tax.TaxObligationEngine;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TaxProfileRepository taxProfileRepository;
    private final TaxObligationEngine taxObligationEngine;
    private final TaxHealthService taxHealthService;

    public BusinessService(BusinessRepository businessRepository,
                           BusinessMembershipRepository membershipRepository,
                           UserRepository userRepository,
                           TaxProfileRepository taxProfileRepository,
                           TaxObligationEngine taxObligationEngine,
                           TaxHealthService taxHealthService) {
        this.businessRepository = businessRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.taxProfileRepository = taxProfileRepository;
        this.taxObligationEngine = taxObligationEngine;
        this.taxHealthService = taxHealthService;
    }

    @Transactional
    public BusinessResponse createBusiness(UUID userId, CreateBusinessRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (businessRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new IllegalArgumentException("Business registration number already exists");
        }

        Business business = Business.builder()
                .name(request.name())
                .legalName(request.legalName())
                .registrationNumber(request.registrationNumber())
                .kraPin(request.kraPin())
                .industry(request.industry())
                .businessType(request.businessType())
                .annualTurnover(request.annualTurnover() == null ? BigDecimal.ZERO : request.annualTurnover())
                .employeeCount(request.employeeCount() == null ? 0 : request.employeeCount())
                .vatRegistered(Boolean.TRUE.equals(request.vatRegistered()))
                .etimsRegistered(Boolean.TRUE.equals(request.etimsRegistered()))
                .countryCode(request.countryCode() == null || request.countryCode().isBlank() ? "KE" : request.countryCode())
                .timezone(request.timezone() == null || request.timezone().isBlank() ? "Africa/Nairobi" : request.timezone())
                .build();

        Business savedBusiness = businessRepository.save(business);

        BusinessMembership membership = BusinessMembership.builder()
                .business(savedBusiness)
                .user(user)
                .role(Role.BUSINESS_OWNER)
                .active(true)
                .build();
        membershipRepository.save(membership);

        int employeeCount = savedBusiness.getEmployeeCount() == null ? 0 : savedBusiness.getEmployeeCount();
        BigDecimal annualTurnover = savedBusiness.getAnnualTurnover() == null ? BigDecimal.ZERO : savedBusiness.getAnnualTurnover();
        taxProfileRepository.save(TaxProfile.builder()
                .business(savedBusiness)
                .businessType(savedBusiness.getBusinessType().name())
                .annualTurnover(annualTurnover)
                .monthlyTurnover(annualTurnover.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP))
                .employeeCount(employeeCount)
                .vatRegistered(savedBusiness.isVatRegistered())
                .etimsRegistered(savedBusiness.isEtimsRegistered())
                .hasEmployees(employeeCount > 0)
                .makesWithholdingPayments(false)
                .receivesRentalIncome(false)
                .importsGoods(false)
                .exportsGoods(false)
                .hasForeignTransactions(false)
                .hasDigitalServices(false)
                .hasExciseActivities(false)
                .incomeTaxApplicable(savedBusiness.getBusinessType() == BusinessType.SOLE_PROPRIETOR
                        || savedBusiness.getBusinessType() == BusinessType.PARTNERSHIP)
                .corporationTaxApplicable(savedBusiness.getBusinessType() == BusinessType.LIMITED_COMPANY
                        || savedBusiness.getBusinessType() == BusinessType.LLP)
                .turnoverTaxApplicable(false)
                .build());

        return toResponse(savedBusiness, List.of(Role.BUSINESS_OWNER.name()));
    }

    @Transactional
    public BusinessResponse updateBusiness(UUID userId, UUID businessId, CreateBusinessRequest request) {
        BusinessMembership membership = requireActiveMembership(userId, businessId);
        Business business = membership.getBusiness();
        if (!business.getRegistrationNumber().equalsIgnoreCase(request.registrationNumber())
                && businessRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new IllegalArgumentException("Business registration number already exists");
        }

        business.setName(request.name());
        business.setLegalName(request.legalName());
        business.setRegistrationNumber(request.registrationNumber());
        business.setKraPin(request.kraPin());
        business.setIndustry(request.industry());
        business.setBusinessType(request.businessType());
        business.setAnnualTurnover(request.annualTurnover() == null ? BigDecimal.ZERO : request.annualTurnover());
        business.setEmployeeCount(request.employeeCount() == null ? 0 : request.employeeCount());
        business.setVatRegistered(Boolean.TRUE.equals(request.vatRegistered()));
        business.setEtimsRegistered(Boolean.TRUE.equals(request.etimsRegistered()));
        business.setCountryCode(request.countryCode() == null || request.countryCode().isBlank()
                ? "KE" : request.countryCode());
        business.setTimezone(request.timezone() == null || request.timezone().isBlank()
                ? "Africa/Nairobi" : request.timezone());

        TaxProfile profile = taxProfileRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new IllegalStateException("Tax profile is missing for this business"));
        if (!profile.getBusinessType().equals(request.businessType().name())) {
            boolean individualBusiness = request.businessType() == BusinessType.SOLE_PROPRIETOR
                    || request.businessType() == BusinessType.PARTNERSHIP;
            boolean corporateBusiness = request.businessType() == BusinessType.LIMITED_COMPANY
                    || request.businessType() == BusinessType.LLP;
            profile.setIncomeTaxApplicable(individualBusiness);
            profile.setCorporationTaxApplicable(corporateBusiness);
        }
        profile.setBusinessType(request.businessType().name());
        profile.setAnnualTurnover(business.getAnnualTurnover());
        profile.setMonthlyTurnover(business.getAnnualTurnover().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
        profile.setEmployeeCount(business.getEmployeeCount());
        profile.setVatRegistered(business.isVatRegistered());
        profile.setEtimsRegistered(business.isEtimsRegistered());
        profile.setHasEmployees(business.getEmployeeCount() > 0);
        taxProfileRepository.save(profile);

        return toResponse(business, List.of(membership.getRole().name()));
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> listBusinessesForUser(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .filter(BusinessMembership::isActive)
                .map(membership -> toResponse(membership.getBusiness(), List.of(membership.getRole().name())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessResponse getBusinessForUser(UUID userId, UUID businessId) {
        BusinessMembership membership = requireActiveMembership(userId, businessId);
        return toResponse(membership.getBusiness(), List.of(membership.getRole().name()));
    }

    @Transactional(readOnly = true)
    public List<TaxObligation> getObligations(UUID userId, UUID businessId) {
        requireActiveMembership(userId, businessId);

        TaxProfile profile = taxProfileRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new IllegalStateException("Tax profile is missing for this business"));
        return taxObligationEngine.determine(toTaxProfile(profile), LocalDate.now(ZoneId.of("Africa/Nairobi")));
    }

    @Transactional(readOnly = true)
    public List<TaxCalendarEntry> getCalendar(UUID userId, UUID businessId) {
        List<TaxObligation> obligations = getObligations(userId, businessId);
        List<TaxCalendarEntry> calendar = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Nairobi"));

        for (TaxObligation obligation : obligations) {
            if (!obligation.applicable() || obligation.nextDueDate() == null) {
                continue;
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, obligation.nextDueDate());
            String status = days < 0 ? "OVERDUE" : (days <= 7 ? "DUE_SOON" : "PENDING");
            calendar.add(new TaxCalendarEntry(obligation.taxType(), obligation.estimatedAmount(), obligation.nextDueDate(), status, days));
        }

        return calendar;
    }

    @Transactional(readOnly = true)
    public com.taxpilot.tax.TaxHealthScore getTaxHealth(UUID userId, UUID businessId) {
        requireActiveMembership(userId, businessId);
        List<TaxCalendarEntry> calendar = getCalendar(userId, businessId);
        long overdueCount = calendar.stream().filter(entry -> "OVERDUE".equals(entry.status())).count();
        TaxProfile profile = taxProfileRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new IllegalStateException("Tax profile is missing for this business"));
        return taxHealthService.evaluate(
                BigDecimal.valueOf(overdueCount),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                profileIssues(profile).isEmpty() ? 0 : 1
        );
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(UUID userId, UUID businessId) {
        BusinessMembership membership = requireActiveMembership(userId, businessId);

        List<TaxCalendarEntry> calendar = getCalendar(userId, businessId);
        List<TaxObligation> obligations = getObligations(userId, businessId);
        TaxHealthScore health = getTaxHealth(userId, businessId);
        TaxProfile storedProfile = taxProfileRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new IllegalStateException("Tax profile is missing for this business"));
        List<String> issues = profileIssues(storedProfile);

        String nextDeadlineLabel = calendar.stream()
                .filter(entry -> entry.dueDate() != null)
                .min(java.util.Comparator.comparing(TaxCalendarEntry::dueDate))
                .map(entry -> entry.taxType() + " — " + entry.dueDate().format(java.time.format.DateTimeFormatter.ofPattern("d MMM")))
                .orElse("No active deadlines");

        BigDecimal estimatedTax = obligations.stream()
                .filter(TaxObligation::applicable)
                .map(TaxObligation::estimatedAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasEstimatedAmount = obligations.stream()
                .filter(TaxObligation::applicable)
                .anyMatch(obligation -> obligation.estimatedAmount() != null);

        List<String> obligationNames = obligations.stream()
                .filter(TaxObligation::applicable)
                .map(TaxObligation::taxType)
                .toList();

        return new DashboardSummaryResponse(
                membership.getBusiness().getName(),
                health.score(),
                health.status(),
                nextDeadlineLabel,
                hasEstimatedAmount ? estimatedTax : null,
                issues.size(),
                issues,
                obligationNames,
                calendar
        );
    }

    private List<String> profileIssues(TaxProfile profile) {
        List<String> issues = new ArrayList<>();
        if (profile.getEmployeeCount() != null && profile.getEmployeeCount() > 0 && !profile.isHasEmployees()) {
            issues.add("Employee count is positive but the profile says there are no employees.");
        }
        if (profile.isIncomeTaxApplicable() && profile.isCorporationTaxApplicable()) {
            issues.add("Both income tax and corporation tax are selected; confirm the business tax classification.");
        }
        if (profile.isTurnoverTaxApplicable()
                && (profile.isIncomeTaxApplicable() || profile.isCorporationTaxApplicable())) {
            issues.add("Turnover tax and an income-based tax are both selected; confirm eligibility with a tax professional.");
        }
        return List.copyOf(issues);
    }

    private BusinessMembership requireActiveMembership(UUID userId, UUID businessId) {
        return membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .filter(BusinessMembership::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
    }

    private BusinessResponse toResponse(Business business, List<String> memberships) {
        return new BusinessResponse(
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
                memberships
        );
    }

    private com.taxpilot.tax.TaxProfile toTaxProfile(TaxProfile profile) {
        return new com.taxpilot.tax.TaxProfile(
                profile.getBusinessType(),
                profile.getAnnualTurnover(),
                profile.getMonthlyTurnover(),
                profile.getEmployeeCount(),
                profile.isVatRegistered(),
                profile.getVatRegistrationDate(),
                profile.isEtimsRegistered(),
                profile.isHasEmployees(),
                profile.isMakesWithholdingPayments(),
                profile.isReceivesRentalIncome(),
                profile.isImportsGoods(),
                profile.isExportsGoods(),
                profile.isHasForeignTransactions(),
                profile.isHasDigitalServices(),
                profile.isHasExciseActivities(),
                profile.isIncomeTaxApplicable(),
                profile.isCorporationTaxApplicable(),
                profile.isTurnoverTaxApplicable()
        );
    }
}
