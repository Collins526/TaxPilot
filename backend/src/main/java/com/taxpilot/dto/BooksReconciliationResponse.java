package com.taxpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BooksReconciliationResponse(
        UUID filingPeriodId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String fileName,
        int transactionCount,
        int salesCount,
        int purchaseCount,
        BigDecimal bookSales,
        BigDecimal purchaseInputVat,
        boolean passed,
        List<BooksReconciliationCheck> checks
) {
}
