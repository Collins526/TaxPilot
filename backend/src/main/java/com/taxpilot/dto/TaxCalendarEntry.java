package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxCalendarEntry(
        String taxType,
        BigDecimal amount,
        LocalDate dueDate,
        String status,
        long daysRemaining
) {
}
