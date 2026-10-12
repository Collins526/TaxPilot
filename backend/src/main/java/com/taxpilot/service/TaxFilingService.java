package com.taxpilot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.business.TaxProfile;
import com.taxpilot.domain.tax.FilingPeriod;
import com.taxpilot.domain.tax.TaxCalculation;
import com.taxpilot.dto.FilingPeriodRequest;
import com.taxpilot.dto.FilingPeriodResponse;
import com.taxpilot.dto.PreKraCheckResponse;
import com.taxpilot.dto.PreKraFinding;
import com.taxpilot.dto.TaxCalculationResponse;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.FilingPeriodRepository;
import com.taxpilot.repository.TaxCalculationRepository;
import com.taxpilot.repository.TaxProfileRepository;
import com.taxpilot.tax.FilingInputs;
import com.taxpilot.tax.TaxCalculationEngine;
import com.taxpilot.tax.TaxComputationResult;

@Service
public class TaxFilingService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.16");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final FilingPeriodRepository filingPeriodRepository;
    private final TaxCalculationRepository taxCalculationRepository;
    private final TaxProfileRepository taxProfileRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final AuditService auditService;

    public TaxFilingService(FilingPeriodRepository filingPeriodRepository,
                            TaxCalculationRepository taxCalculationRepository,
                            TaxProfileRepository taxProfileRepository,
                            BusinessMembershipRepository membershipRepository,
                            TaxCalculationEngine taxCalculationEngine,
                            AuditService auditService) {
        this.filingPeriodRepository = filingPeriodRepository;
        this.taxCalculationRepository = taxCalculationRepository;
        this.taxProfileRepository = taxProfileRepository;
        this.membershipRepository = membershipRepository;
        this.taxCalculationEngine = taxCalculationEngine;
        this.auditService = auditService;
    }

    @Transactional
    public FilingPeriodResponse createFilingPeriod(UUID userId, UUID businessId, FilingPeriodRequest request) {
        Business business = requireActiveMembership(userId, businessId).getBusiness();
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new IllegalArgumentException("Period end must not be before period start");
        }
        if (filingPeriodRepository.existsByBusinessIdAndPeriodStartAndPeriodEnd(
                businessId, request.periodStart(), request.periodEnd())) {
            throw new IllegalArgumentException("A filing period for this range already exists");
        }

        FilingPeriod period = new FilingPeriod();
        period.setBusiness(business);
        period.setPeriodStart(request.periodStart());
        period.setPeriodEnd(request.periodEnd());
        period.setTaxableSales(request.taxableSales());
        period.setInputVat(request.inputVat());
        period.setPayrollGross(request.payrollGross());
        period.setWithholdingBase(request.withholdingBase());
        period.setTaxableProfit(request.taxableProfit());
        period.setEtimsDeclaredSales(request.etimsDeclaredSales());
        period.setNotes(request.notes());
        period.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        period.setCreatedBy(userId);
        FilingPeriod saved = filingPeriodRepository.save(period);

        List<TaxCalculation> calculations = computeAndStore(business, saved);
        auditService.record(AuditAction.FILING_PERIOD_CREATED, "FILING_PERIOD", saved.getId(), businessId,
                "Filing period " + saved.getPeriodStart() + " to " + saved.getPeriodEnd()
                        + " recorded with " + calculations.size() + " calculation(s)");

        return toResponse(saved, calculations);
    }

    @Transactional(readOnly = true)
    public List<FilingPeriodResponse> listFilingPeriods(UUID userId, UUID businessId) {
        requireActiveMembership(userId, businessId);
        return filingPeriodRepository.findByBusinessIdOrderByPeriodEndDesc(businessId).stream()
                .map(period -> toResponse(period, taxCalculationRepository.findByFilingPeriodIdOrderByTaxTypeAsc(period.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public FilingPeriodResponse getFilingPeriod(UUID userId, UUID businessId, UUID periodId) {
        requireActiveMembership(userId, businessId);
        FilingPeriod period = filingPeriodRepository.findByIdAndBusinessId(periodId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Filing period not found"));
        return toResponse(period, taxCalculationRepository.findByFilingPeriodIdOrderByTaxTypeAsc(periodId));
    }

    @Transactional(readOnly = true)
    public PreKraCheckResponse runPreKraCheck(UUID userId, UUID businessId) {
        Business business = requireActiveMembership(userId, businessId).getBusiness();
        TaxProfile profile = taxProfileRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new IllegalStateException("Tax profile is missing for this business"));

        FilingPeriod period = filingPeriodRepository.findFirstByBusinessIdOrderByPeriodEndDesc(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Record a filing period before running the Pre-KRA check"));

        List<PreKraFinding> findings = new ArrayList<>();

        if (profile.isVatRegistered()) {
            checkVat(period, findings);
        }
        checkEtims(profile, period, findings);
        checkWithholding(profile, period, findings);
        checkSalesPresence(period, findings);

        return new PreKraCheckResponse(businessId, period.getId(), period.getPeriodEnd(),
                findings.isEmpty(), findings.size(), List.copyOf(findings));
    }

    private void checkVat(FilingPeriod period, List<PreKraFinding> findings) {
        BigDecimal sales = period.getTaxableSales();
        if (sales == null || sales.signum() == 0) {
            return;
        }
        BigDecimal expectedOutput = money(sales.multiply(VAT_RATE));
        BigDecimal inputVat = period.getInputVat() == null ? BigDecimal.ZERO : period.getInputVat();
        if (inputVat.compareTo(expectedOutput) > 0) {
            findings.add(new PreKraFinding("WARNING", "VAT",
                    "Input VAT " + money(inputVat) + " exceeds expected output VAT " + expectedOutput
                            + " on recorded sales; this is a credit/refund position — verify before filing."));
        }
    }

    private void checkEtims(TaxProfile profile, FilingPeriod period, List<PreKraFinding> findings) {
        if (!profile.isEtimsRegistered()) {
            return;
        }
        BigDecimal declared = period.getEtimsDeclaredSales();
        BigDecimal sales = period.getTaxableSales();
        if (declared == null) {
            findings.add(new PreKraFinding("INFO", "ETIMS",
                    "This business is eTIMS-registered but no eTIMS declared sales were recorded for the period."));
            return;
        }
        if (sales == null) {
            return;
        }
        BigDecimal difference = declared.subtract(sales).abs();
        BigDecimal allowed = sales.abs().multiply(TOLERANCE);
        if (difference.compareTo(allowed) > 0) {
            findings.add(new PreKraFinding("HIGH", "ETIMS",
                    "eTIMS declared sales " + money(declared) + " differ from recorded sales " + money(sales)
                            + " by " + money(difference) + " — reconcile before submitting."));
        }
    }

    private void checkWithholding(TaxProfile profile, FilingPeriod period, List<PreKraFinding> findings) {
        if (!profile.isMakesWithholdingPayments()) {
            return;
        }
        if (period.getWithholdingBase() == null || period.getWithholdingBase().signum() == 0) {
            findings.add(new PreKraFinding("WARNING", "WITHHOLDING_TAX",
                    "The profile indicates withholding obligations but no withholding base was recorded for this period."));
        }
    }

    private void checkSalesPresence(FilingPeriod period, List<PreKraFinding> findings) {
        if (period.getTaxableSales() == null) {
            findings.add(new PreKraFinding("INFO", "SALES",
                    "No taxable sales were recorded for this period; calculations depending on sales are unavailable."));
        }
    }

    private List<TaxCalculation> computeAndStore(Business business, FilingPeriod period) {
        TaxProfile profile = taxProfileRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new IllegalStateException("Tax profile is missing for this business"));

        FilingInputs inputs = new FilingInputs(
                period.getTaxableSales(),
                period.getInputVat(),
                period.getPayrollGross(),
                period.getWithholdingBase(),
                period.getTaxableProfit()
        );

        LocalDate asOf = period.getPeriodEnd() == null ? LocalDate.now() : period.getPeriodEnd();
        List<TaxCalculation> saved = new ArrayList<>();
        for (TaxComputationResult result : taxCalculationEngine.compute(toTaxProfile(profile), inputs, asOf)) {
            TaxCalculation calculation = new TaxCalculation();
            calculation.setFilingPeriod(period);
            calculation.setBusinessId(business.getId());
            calculation.setTaxType(result.taxType());
            calculation.setRuleCode(result.ruleCode());
            calculation.setRuleVersion(result.ruleVersion());
            calculation.setRate(result.rate());
            calculation.setAmount(result.amount());
            calculation.setBasis(result.basis());
            calculation.setComputedAt(LocalDateTime.now(ZoneOffset.UTC));
            saved.add(taxCalculationRepository.save(calculation));
        }
        return saved;
    }

    private FilingPeriodResponse toResponse(FilingPeriod period, List<TaxCalculation> calculations) {
        BigDecimal total = calculations.stream()
                .map(TaxCalculation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TaxCalculationResponse> responses = calculations.stream()
                .map(calculation -> new TaxCalculationResponse(
                        calculation.getTaxType(),
                        calculation.getRuleCode(),
                        calculation.getRuleVersion(),
                        calculation.getRate(),
                        calculation.getAmount(),
                        calculation.getBasis()))
                .toList();
        return new FilingPeriodResponse(
                period.getId(),
                period.getPeriodStart(),
                period.getPeriodEnd(),
                period.getTaxableSales(),
                period.getInputVat(),
                period.getPayrollGross(),
                period.getWithholdingBase(),
                period.getTaxableProfit(),
                period.getEtimsDeclaredSales(),
                period.getNotes(),
                period.getCreatedAt(),
                calculations.isEmpty() ? null : total,
                responses
        );
    }

    private BusinessMembership requireActiveMembership(UUID userId, UUID businessId) {
        return membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .filter(BusinessMembership::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
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

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
