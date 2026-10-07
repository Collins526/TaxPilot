package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.taxpilot.tax.TaxHealthScore;
import com.taxpilot.tax.TaxHealthService;
import com.taxpilot.tax.TaxObligation;
import com.taxpilot.tax.TaxObligationEngine;
import com.taxpilot.tax.TaxProfile;
import com.taxpilot.domain.tax.TaxRuleVersion;
import com.taxpilot.repository.TaxRuleVersionRepository;
import org.mockito.Mockito;

class TaxDomainEngineTest {

    private final TaxRuleVersionRepository taxRuleVersionRepository = Mockito.mock(TaxRuleVersionRepository.class);
    private final TaxObligationEngine obligationEngine = new TaxObligationEngine(taxRuleVersionRepository);
    private final TaxHealthService taxHealthService = new TaxHealthService();

    @Test
    void determinesVatAndPayeObligations() {
        TaxRuleVersion payeRule = rule("PAYE", 9);
        TaxRuleVersion vatRule = rule("VAT", 20);
        Mockito.when(taxRuleVersionRepository.findByRuleCodeIn(Mockito.anyList()))
                .thenReturn(List.of(payeRule, vatRule));
        TaxProfile profile = new TaxProfile(
                "LIMITED_COMPANY",
                new BigDecimal("8000000"),
                new BigDecimal("666666.67"),
                10,
                true,
                LocalDate.of(2026, 7, 1),
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                false
        );

        List<TaxObligation> obligations = obligationEngine.determine(profile, LocalDate.of(2026, 11, 1));

        assertFalse(obligations.isEmpty());
        assertEquals("PAYE", obligations.get(0).taxType());
        assertEquals("VAT", obligations.stream().filter(o -> o.taxType().equals("VAT")).findFirst().orElseThrow().taxType());
        assertEquals(LocalDate.of(2026, 11, 9), obligations.get(0).nextDueDate());
        assertEquals("KE-CALENDAR-2026.1", obligations.get(0).ruleVersion());
        assertNull(obligations.get(0).estimatedAmount());
    }

    private TaxRuleVersion rule(String code, int dueDay) {
        TaxRuleVersion rule = new TaxRuleVersion();
        rule.setRuleCode(code);
        rule.setRuleVersion("KE-CALENDAR-2026.1");
        rule.setFrequency("MONTHLY");
        rule.setDueDay(dueDay);
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setSource("KRA guidance");
        return rule;
    }

    @Test
    void calculatesTaxHealthScore() {
        TaxHealthScore score = taxHealthService.evaluate(
                new BigDecimal("30000"),
                1,
                0,
                0,
                1,
                0,
                0,
                1,
                0,
                0,
                0
        );

        assertNotNull(score);
        assertEquals(70, score.score());
    }

}
