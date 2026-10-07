package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxProfileResponse(
        String businessType,
        BigDecimal annualTurnover,
        BigDecimal monthlyTurnover,
        Integer employeeCount,
        boolean vatRegistered,
        LocalDate vatRegistrationDate,
        boolean etimsRegistered,
        boolean hasEmployees,
        boolean makesWithholdingPayments,
        boolean receivesRentalIncome,
        boolean importsGoods,
        boolean exportsGoods,
        boolean hasForeignTransactions,
        boolean hasDigitalServices,
        boolean hasExciseActivities,
        boolean incomeTaxApplicable,
        boolean corporationTaxApplicable,
        boolean turnoverTaxApplicable
) {
}
