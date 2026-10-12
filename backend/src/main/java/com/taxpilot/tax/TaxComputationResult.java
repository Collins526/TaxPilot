package com.taxpilot.tax;

import java.math.BigDecimal;

public record TaxComputationResult(
        String taxType,
        String ruleCode,
        String ruleVersion,
        BigDecimal rate,
        BigDecimal amount,
        String basis
) {
}
