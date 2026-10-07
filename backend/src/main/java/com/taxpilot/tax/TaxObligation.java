package com.taxpilot.tax;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxObligation(
        String taxType,
        boolean applicable,
        String reason,
        String frequency,
        LocalDate nextDueDate,
        BigDecimal estimatedAmount,
        String ruleVersion
) {
}
