package com.taxpilot.dto;

import java.math.BigDecimal;

public record TaxCalculationResponse(
        String taxType,
        String ruleCode,
        String ruleVersion,
        BigDecimal rate,
        BigDecimal amount,
        String basis
) {
}
