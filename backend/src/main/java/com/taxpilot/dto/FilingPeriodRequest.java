package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FilingPeriodRequest(
        @NotNull(message = "Period start is required") LocalDate periodStart,
        @NotNull(message = "Period end is required") LocalDate periodEnd,
        @DecimalMin(value = "0.00", message = "Taxable sales must be zero or greater") BigDecimal taxableSales,
        @DecimalMin(value = "0.00", message = "Input VAT must be zero or greater") BigDecimal inputVat,
        @DecimalMin(value = "0.00", message = "Gross payroll must be zero or greater") BigDecimal payrollGross,
        @DecimalMin(value = "0.00", message = "Withholding base must be zero or greater") BigDecimal withholdingBase,
        @DecimalMin(value = "0.00", message = "Taxable profit must be zero or greater") BigDecimal taxableProfit,
        @DecimalMin(value = "0.00", message = "eTIMS declared sales must be zero or greater") BigDecimal etimsDeclaredSales,
        @Size(max = 512, message = "Notes must be 512 characters or fewer") String notes
) {
}
