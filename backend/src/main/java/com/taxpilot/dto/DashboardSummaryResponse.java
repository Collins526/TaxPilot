package com.taxpilot.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        String businessName,
        int taxHealthScore,
        String taxHealthStatus,
        String nextDeadlineLabel,
        BigDecimal estimatedTax,
        int issuesFound,
        List<String> complianceIssues,
        List<String> obligations,
        List<TaxCalendarEntry> upcomingDeadlines
) {
}
