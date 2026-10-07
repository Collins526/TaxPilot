package com.taxpilot.tax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TaxHealthService {

    public TaxHealthScore evaluate(BigDecimal overdueObligations,
                                 int missingReturns,
                                 int missingDocuments,
                                 int etimsMismatches,
                                 int vatInconsistencies,
                                 int payeInconsistencies,
                                 int whtInconsistencies,
                                 int missingInvoices,
                                 int unexplainedTransactions,
                                 int filingHistoryIssues,
                                 int reconciliationIssues) {
        List<String> issues = new ArrayList<>();

        if (overdueObligations != null && overdueObligations.compareTo(BigDecimal.ZERO) > 0) {
            issues.add("overdue obligations");
        }
        if (missingReturns > 0) {
            issues.add("missing returns");
        }
        if (missingDocuments > 0) {
            issues.add("missing documents");
        }
        if (etimsMismatches > 0) {
            issues.add("eTIMS mismatches");
        }
        if (vatInconsistencies > 0) {
            issues.add("VAT inconsistencies");
        }
        if (payeInconsistencies > 0) {
            issues.add("PAYE inconsistencies");
        }
        if (whtInconsistencies > 0) {
            issues.add("WHT inconsistencies");
        }
        if (missingInvoices > 0) {
            issues.add("missing invoices");
        }
        if (unexplainedTransactions > 0) {
            issues.add("unexplained transactions");
        }
        if (filingHistoryIssues > 0) {
            issues.add("filing history");
        }
        if (reconciliationIssues > 0) {
            issues.add("reconciliation status");
        }

        int overduePenalty = overdueObligations == null ? 0 : overdueObligations
                .divide(BigDecimal.valueOf(15000), 0, java.math.RoundingMode.HALF_UP)
                .intValue();
        int totalIssueCount = issues.size();
        int criticalIssues = Math.max(0, totalIssueCount - 2);

        int score = 100 - (totalIssueCount * 7) - overduePenalty;
        if (score < 0) {
            score = 0;
        }
        if (score > 100) {
            score = 100;
        }

        String status = switch (score) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> "POOR";
            case 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69 -> "AT_RISK";
            default -> "GOOD";
        };

        return new TaxHealthScore(score, status, issues.size(), criticalIssues);
    }
}
