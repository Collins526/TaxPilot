package com.taxpilot.tax;

import java.math.BigDecimal;

public record FilingInputs(
        BigDecimal taxableSales,
        BigDecimal inputVat,
        BigDecimal payrollGross,
        BigDecimal withholdingBase,
        BigDecimal taxableProfit
) {
}
