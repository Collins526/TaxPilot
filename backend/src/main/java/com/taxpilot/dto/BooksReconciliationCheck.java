package com.taxpilot.dto;

import java.math.BigDecimal;

public record BooksReconciliationCheck(
        String code,
        BigDecimal booksAmount,
        BigDecimal recordedAmount,
        BigDecimal difference,
        String status,
        String message
) {
}
