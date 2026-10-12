package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FilingPeriodResponse(
        UUID id,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal taxableSales,
        BigDecimal inputVat,
        BigDecimal payrollGross,
        BigDecimal withholdingBase,
        BigDecimal taxableProfit,
        BigDecimal etimsDeclaredSales,
        String notes,
        LocalDateTime createdAt,
        BigDecimal totalEstimatedTax,
        List<TaxCalculationResponse> calculations
) {
}
