package com.taxpilot.service;

import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.domain.business.TaxProfile;
import com.taxpilot.dto.TaxProfileRequest;
import com.taxpilot.dto.TaxProfileResponse;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.TaxProfileRepository;

@Service
public class TaxProfileService {

    private final TaxProfileRepository taxProfileRepository;
    private final BusinessMembershipRepository membershipRepository;

    public TaxProfileService(TaxProfileRepository taxProfileRepository, BusinessMembershipRepository membershipRepository) {
        this.taxProfileRepository = taxProfileRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public TaxProfileResponse getTaxProfile(UUID userId, UUID businessId) {
        Business business = requireActiveMembership(userId, businessId).getBusiness();
        TaxProfile profile = taxProfileRepository.findByBusinessId(businessId).orElseGet(() -> defaultProfile(business));
        return toResponse(profile);
    }

    @Transactional
    public TaxProfileResponse saveTaxProfile(UUID userId, UUID businessId, TaxProfileRequest request) {
        Business business = requireActiveMembership(userId, businessId).getBusiness();

        TaxProfile profile = taxProfileRepository.findByBusinessId(businessId)
                .orElseGet(() -> defaultProfile(business));

        profile.setBusiness(business);
        profile.setBusinessType(business.getBusinessType().name());
        BigDecimal annualTurnover = request.annualTurnover() == null ? BigDecimal.ZERO : request.annualTurnover();
        profile.setAnnualTurnover(annualTurnover);
        profile.setMonthlyTurnover(request.monthlyTurnover() == null
                ? annualTurnover.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : request.monthlyTurnover());
        profile.setEmployeeCount(request.employeeCount());
        profile.setVatRegistered(Boolean.TRUE.equals(request.vatRegistered()));
        profile.setVatRegistrationDate(request.vatRegistrationDate());
        profile.setEtimsRegistered(Boolean.TRUE.equals(request.etimsRegistered()));
        profile.setHasEmployees(Boolean.TRUE.equals(request.hasEmployees()));
        profile.setMakesWithholdingPayments(Boolean.TRUE.equals(request.makesWithholdingPayments()));
        profile.setReceivesRentalIncome(Boolean.TRUE.equals(request.receivesRentalIncome()));
        profile.setImportsGoods(Boolean.TRUE.equals(request.importsGoods()));
        profile.setExportsGoods(Boolean.TRUE.equals(request.exportsGoods()));
        profile.setHasForeignTransactions(Boolean.TRUE.equals(request.hasForeignTransactions()));
        profile.setHasDigitalServices(Boolean.TRUE.equals(request.hasDigitalServices()));
        profile.setHasExciseActivities(Boolean.TRUE.equals(request.hasExciseActivities()));
        profile.setIncomeTaxApplicable(Boolean.TRUE.equals(request.incomeTaxApplicable()));
        profile.setCorporationTaxApplicable(Boolean.TRUE.equals(request.corporationTaxApplicable()));
        profile.setTurnoverTaxApplicable(Boolean.TRUE.equals(request.turnoverTaxApplicable()));

        business.setAnnualTurnover(annualTurnover);
        business.setEmployeeCount(request.employeeCount());
        business.setVatRegistered(profile.isVatRegistered());
        business.setEtimsRegistered(profile.isEtimsRegistered());

        TaxProfile saved = taxProfileRepository.save(profile);
        return toResponse(saved);
    }

    private BusinessMembership requireActiveMembership(UUID userId, UUID businessId) {
        BusinessMembership membership = membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .filter(BusinessMembership::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        return membership;
    }

    private TaxProfile defaultProfile(Business business) {
        int employeeCount = business.getEmployeeCount() == null ? 0 : business.getEmployeeCount();
        BigDecimal annualTurnover = business.getAnnualTurnover() == null ? BigDecimal.ZERO : business.getAnnualTurnover();
        BusinessType type = business.getBusinessType();
        return TaxProfile.builder()
                .business(business)
                .businessType(type == null ? BusinessType.OTHER.name() : type.name())
                .annualTurnover(annualTurnover)
                .monthlyTurnover(annualTurnover.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP))
                .employeeCount(employeeCount)
                .vatRegistered(business.isVatRegistered())
                .etimsRegistered(business.isEtimsRegistered())
                .hasEmployees(employeeCount > 0)
                .incomeTaxApplicable(type == BusinessType.SOLE_PROPRIETOR || type == BusinessType.PARTNERSHIP)
                .corporationTaxApplicable(type == BusinessType.LIMITED_COMPANY || type == BusinessType.LLP)
                .build();
    }

    private TaxProfileResponse toResponse(TaxProfile profile) {
        return new TaxProfileResponse(
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
