package com.taxpilot.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.taxpilot.domain.tax.TaxRuleBand;
import com.taxpilot.domain.tax.TaxRuleVersion;
import com.taxpilot.repository.TaxRuleBandRepository;

@Component
public class TaxCalculationEngine {

    private static final int MONEY_SCALE = 2;

    private final TaxObligationEngine taxObligationEngine;
    private final TaxRuleBandRepository taxRuleBandRepository;

    public TaxCalculationEngine(TaxObligationEngine taxObligationEngine,
                                TaxRuleBandRepository taxRuleBandRepository) {
        this.taxObligationEngine = taxObligationEngine;
        this.taxRuleBandRepository = taxRuleBandRepository;
    }

    public List<TaxComputationResult> compute(TaxProfile profile, FilingInputs inputs, LocalDate asOf) {
        Map<String, TaxRuleVersion> rules = taxObligationEngine.activeRules(asOf);
        List<TaxComputationResult> results = new ArrayList<>();

        for (TaxObligation obligation : taxObligationEngine.determine(profile, asOf)) {
            TaxRuleVersion rule = rules.get(obligation.taxType());
            if (rule == null) {
                continue;
            }
            TaxComputationResult result = computeObligation(rule, obligation.taxType(), inputs);
            if (result != null) {
                results.add(result);
            }
        }
        return List.copyOf(results);
    }

    private TaxComputationResult computeObligation(TaxRuleVersion rule, String taxType, FilingInputs inputs) {
        return switch (taxType) {
            case "VAT" -> computeVat(rule, inputs);
            case "PAYE" -> computeProgressive(rule, taxType, inputs.payrollGross(), "gross payroll");
            case "INCOME_TAX" -> computeProgressive(rule, taxType, inputs.taxableProfit(), "taxable profit");
            case "CORPORATION_TAX" -> computeFlat(rule, taxType, inputs.taxableProfit(), "taxable profit");
            case "TURNOVER_TAX" -> computeFlat(rule, taxType, inputs.taxableSales(), "taxable sales");
            case "WITHHOLDING_TAX" -> computeFlat(rule, taxType, inputs.withholdingBase(), "withholding base");
            default -> null;
        };
    }

    private TaxComputationResult computeVat(TaxRuleVersion rule, FilingInputs inputs) {
        if (inputs.taxableSales() == null || rule.getRate() == null) {
            return null;
        }
        BigDecimal output = money(inputs.taxableSales().multiply(rule.getRate()));
        BigDecimal input = inputs.inputVat() == null ? BigDecimal.ZERO : money(inputs.inputVat());
        BigDecimal net = output.subtract(input);
        if (net.signum() < 0) {
            net = BigDecimal.ZERO;
        }
        String basis = "Output VAT " + output + " minus input VAT " + input + " at rate " + rule.getRate().toPlainString();
        return new TaxComputationResult("VAT", rule.getRuleCode(), rule.getRuleVersion(), rule.getRate(), money(net), basis);
    }

    private TaxComputationResult computeFlat(TaxRuleVersion rule, String taxType, BigDecimal base, String baseLabel) {
        if (base == null || rule.getRate() == null) {
            return null;
        }
        BigDecimal amount = money(base.multiply(rule.getRate()));
        String basis = baseLabel + " " + base + " at rate " + rule.getRate().toPlainString();
        return new TaxComputationResult(taxType, rule.getRuleCode(), rule.getRuleVersion(), rule.getRate(), amount, basis);
    }

    private TaxComputationResult computeProgressive(TaxRuleVersion rule, String taxType, BigDecimal income, String baseLabel) {
        if (income == null) {
            return null;
        }
        List<TaxRuleBand> bands = taxRuleBandRepository
                .findByRuleCodeAndRuleVersionOrderByBandOrderAsc(rule.getRuleCode(), rule.getRuleVersion());
        if (bands.isEmpty()) {
            return null;
        }

        BigDecimal gross = BigDecimal.ZERO;
        for (TaxRuleBand band : bands) {
            BigDecimal upper = band.getUpperBound() == null ? income : band.getUpperBound();
            BigDecimal slice = income.min(upper).subtract(band.getLowerBound());
            if (slice.signum() > 0) {
                gross = gross.add(slice.multiply(band.getRate()));
            }
        }

        BigDecimal relief = rule.getThreshold() == null ? BigDecimal.ZERO : rule.getThreshold();
        BigDecimal taxable = gross.subtract(relief);
        if (taxable.signum() < 0) {
            taxable = BigDecimal.ZERO;
        }
        String basis = "Progressive bands on " + baseLabel + " " + income
                + ", less relief " + relief + " (schedule " + rule.getRuleVersion() + ")";
        return new TaxComputationResult(taxType, rule.getRuleCode(), rule.getRuleVersion(), null, money(taxable), basis);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
