package com.taxpilot.tax;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.taxpilot.domain.tax.TaxRuleVersion;
import com.taxpilot.repository.TaxRuleVersionRepository;

@Component
public class TaxObligationEngine {

    private final TaxRuleVersionRepository taxRuleVersionRepository;

    public TaxObligationEngine(TaxRuleVersionRepository taxRuleVersionRepository) {
        this.taxRuleVersionRepository = taxRuleVersionRepository;
    }

    public List<TaxObligation> determine(TaxProfile profile, LocalDate asOf) {
        LocalDate referenceDate = asOf == null ? LocalDate.now() : asOf;
        List<String> ruleCodes = List.of("PAYE", "VAT", "WITHHOLDING_TAX", "INCOME_TAX", "CORPORATION_TAX", "TURNOVER_TAX");
        Map<String, TaxRuleVersion> activeRules = taxRuleVersionRepository.findByRuleCodeIn(ruleCodes).stream()
                .filter(rule -> !rule.getEffectiveFrom().isAfter(referenceDate))
                .filter(rule -> rule.getEffectiveTo() == null || !rule.getEffectiveTo().isBefore(referenceDate))
                .sorted(Comparator.comparing(TaxRuleVersion::getEffectiveFrom)
                        .thenComparing(TaxRuleVersion::getRuleVersion))
                .collect(Collectors.toMap(TaxRuleVersion::getRuleCode, Function.identity(),
                        (first, second) -> second));

        List<TaxObligation> obligations = new ArrayList<>();
        addIfApplicable(obligations, profile.hasEmployees(), "PAYE", "Business reports employees", activeRules, referenceDate);
        addIfApplicable(obligations, profile.vatRegistered(), "VAT", "Business is VAT registered", activeRules, referenceDate);
        addIfApplicable(obligations, profile.makesWithholdingPayments(), "WITHHOLDING_TAX",
                "Business reports making withholding payments", activeRules, referenceDate);
        addIfApplicable(obligations, profile.incomeTaxApplicable(), "INCOME_TAX",
                "Business tax profile indicates income tax applies", activeRules, referenceDate);
        addIfApplicable(obligations, profile.corporationTaxApplicable(), "CORPORATION_TAX",
                "Business tax profile indicates corporation tax applies", activeRules, referenceDate);
        addIfApplicable(obligations, profile.turnoverTaxApplicable(), "TURNOVER_TAX",
                "Business tax profile indicates turnover tax applies", activeRules, referenceDate);
        return List.copyOf(obligations);
    }

    private void addIfApplicable(List<TaxObligation> obligations,
                                 boolean applicable,
                                 String taxType,
                                 String reason,
                                 Map<String, TaxRuleVersion> activeRules,
                                 LocalDate referenceDate) {
        if (!applicable) {
            return;
        }

        TaxRuleVersion rule = activeRules.get(taxType);
        String frequency = rule == null ? "UNCONFIGURED" : rule.getFrequency();
        LocalDate dueDate = rule == null || rule.getDueDay() == null
                ? null
                : nextMonthlyDueDate(rule.getDueDay(), referenceDate);
        obligations.add(new TaxObligation(
                taxType,
                true,
                reason,
                frequency,
                dueDate,
                null,
                rule == null ? null : rule.getRuleVersion()
        ));
    }

    private LocalDate nextMonthlyDueDate(int dueDay, LocalDate referenceDate) {
        YearMonth month = YearMonth.from(referenceDate);
        LocalDate dueDate = month.atDay(Math.min(dueDay, month.lengthOfMonth()));
        if (dueDate.isBefore(referenceDate)) {
            month = month.plusMonths(1);
            dueDate = month.atDay(Math.min(dueDay, month.lengthOfMonth()));
        }
        return dueDate;
    }
}
