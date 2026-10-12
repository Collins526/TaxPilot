package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.taxpilot.domain.tax.TaxRuleBand;
import com.taxpilot.domain.tax.TaxRuleVersion;
import com.taxpilot.repository.TaxRuleBandRepository;
import com.taxpilot.tax.FilingInputs;
import com.taxpilot.tax.TaxCalculationEngine;
import com.taxpilot.tax.TaxComputationResult;
import com.taxpilot.tax.TaxObligation;
import com.taxpilot.tax.TaxObligationEngine;
import com.taxpilot.tax.TaxProfile;

class TaxCalculationEngineTest {

    private final TaxObligationEngine obligationEngine = Mockito.mock(TaxObligationEngine.class);
    private final TaxRuleBandRepository bandRepository = Mockito.mock(TaxRuleBandRepository.class);
    private final TaxCalculationEngine engine = new TaxCalculationEngine(obligationEngine, bandRepository);

    @Test
    void computesVatNetOfInput() {
        TaxRuleVersion vat = rule("VAT", "0.16", null);
        applyRules(Map.of("VAT", vat), "VAT");

        List<TaxComputationResult> results = engine.compute(profile(), new FilingInputs(
                new BigDecimal("100000"), new BigDecimal("6000"), null, null, null), LocalDate.of(2026, 6, 30));

        assertEquals(1, results.size());
        assertEquals(new BigDecimal("10000.00"), results.get(0).amount());
        assertEquals("0.16", results.get(0).rate().toPlainString());
        assertEquals("KE-CALENDAR-2026.1", results.get(0).ruleVersion());
    }

    @Test
    void computesPayeProgressivelyLessRelief() {
        TaxRuleVersion paye = rule("PAYE", null, "2400");
        applyRules(Map.of("PAYE", paye), "PAYE");
        Mockito.when(bandRepository.findByRuleCodeAndRuleVersionOrderByBandOrderAsc("PAYE", "KE-CALENDAR-2026.1"))
                .thenReturn(List.of(band(1, "0", "24000", "0.10"), band(2, "24000", "32333", "0.25")));

        List<TaxComputationResult> results = engine.compute(profile(), new FilingInputs(
                null, null, new BigDecimal("30000"), null, null), LocalDate.of(2026, 6, 30));

        assertEquals(1, results.size());
        assertEquals(new BigDecimal("1500.00"), results.get(0).amount());
        assertEquals(null, results.get(0).rate());
    }

    @Test
    void computesCorporationTaxFlat() {
        TaxRuleVersion corp = rule("CORPORATION_TAX", "0.30", null);
        applyRules(Map.of("CORPORATION_TAX", corp), "CORPORATION_TAX");

        List<TaxComputationResult> results = engine.compute(profile(), new FilingInputs(
                null, null, null, null, new BigDecimal("1000000")), LocalDate.of(2026, 6, 30));

        assertEquals(1, results.size());
        assertEquals(new BigDecimal("300000.00"), results.get(0).amount());
    }

    @Test
    void skipsTaxesWithMissingInputs() {
        TaxRuleVersion vat = rule("VAT", "0.16", null);
        applyRules(Map.of("VAT", vat), "VAT");

        List<TaxComputationResult> results = engine.compute(profile(), new FilingInputs(
                null, null, null, null, null), LocalDate.of(2026, 6, 30));

        assertTrue(results.isEmpty());
    }

    private void applyRules(Map<String, TaxRuleVersion> rules, String... applicable) {
        Mockito.when(obligationEngine.activeRules(Mockito.any())).thenReturn(rules);
        List<TaxObligation> obligations = java.util.Arrays.stream(applicable)
                .map(code -> new TaxObligation(code, true, "test", "MONTHLY", null, null, "KE-CALENDAR-2026.1"))
                .toList();
        Mockito.when(obligationEngine.determine(Mockito.any(), Mockito.any())).thenReturn(obligations);
    }

    private TaxRuleVersion rule(String code, String rate, String threshold) {
        TaxRuleVersion rule = new TaxRuleVersion();
        rule.setRuleCode(code);
        rule.setRuleVersion("KE-CALENDAR-2026.1");
        rule.setFrequency("MONTHLY");
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setSource("KRA guidance");
        rule.setRate(rate == null ? null : new BigDecimal(rate));
        rule.setThreshold(threshold == null ? null : new BigDecimal(threshold));
        return rule;
    }

    private TaxRuleBand band(int order, String lower, String upper, String rate) {
        TaxRuleBand band = new TaxRuleBand();
        band.setBandOrder(order);
        band.setLowerBound(new BigDecimal(lower));
        band.setUpperBound(upper == null ? null : new BigDecimal(upper));
        band.setRate(new BigDecimal(rate));
        return band;
    }

    private TaxProfile profile() {
        return new TaxProfile("LIMITED_COMPANY", new BigDecimal("8000000"), new BigDecimal("666666.67"),
                10, true, LocalDate.of(2026, 7, 1), true, true, false, false,
                false, false, false, false, false, true, true, false);
    }
}
