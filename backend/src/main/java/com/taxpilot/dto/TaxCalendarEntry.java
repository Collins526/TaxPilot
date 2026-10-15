package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.taxpilot.domain.tax.FilingStatus;

public record TaxCalendarEntry(
        String taxType,
        BigDecimal amount,
        LocalDate dueDate,
        String status,
        long daysRemaining,
        FilingStatus filingStatus
) {
}
