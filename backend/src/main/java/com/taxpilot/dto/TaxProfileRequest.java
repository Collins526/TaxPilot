package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record TaxProfileRequest(
        @NotBlank(message = "Business type is required") String businessType,
        @DecimalMin(value = "0.00", message = "Annual turnover must be zero or greater") BigDecimal annualTurnover,
        @DecimalMin(value = "0.00", message = "Monthly turnover must be zero or greater") BigDecimal monthlyTurnover,
        @Min(value = 0, message = "Employee count must be zero or greater") Integer employeeCount,
        @NotNull(message = "VAT registration status is required") Boolean vatRegistered,
        LocalDate vatRegistrationDate,
        @NotNull(message = "eTIMS status is required") Boolean etimsRegistered,
        @NotNull(message = "Has employees flag is required") Boolean hasEmployees,
        @NotNull(message = "Withholding payment flag is required") Boolean makesWithholdingPayments,
        @NotNull(message = "Rental income flag is required") Boolean receivesRentalIncome,
        @NotNull(message = "Imports goods flag is required") Boolean importsGoods,
        @NotNull(message = "Exports goods flag is required") Boolean exportsGoods,
        @NotNull(message = "Foreign transaction flag is required") Boolean hasForeignTransactions,
        @NotNull(message = "Digital services flag is required") Boolean hasDigitalServices,
        @NotNull(message = "Excise activity flag is required") Boolean hasExciseActivities,
        @NotNull(message = "Income tax applicability flag is required") Boolean incomeTaxApplicable,
        @NotNull(message = "Corporation tax applicability flag is required") Boolean corporationTaxApplicable,
        @NotNull(message = "Turnover tax applicability flag is required") Boolean turnoverTaxApplicable
) {
}
