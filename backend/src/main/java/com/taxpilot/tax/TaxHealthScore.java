package com.taxpilot.tax;

public record TaxHealthScore(
        int score,
        String status,
        int issues,
        int criticalIssues
) {
}
