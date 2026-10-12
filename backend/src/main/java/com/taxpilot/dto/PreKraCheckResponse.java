package com.taxpilot.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PreKraCheckResponse(
        UUID businessId,
        UUID filingPeriodId,
        LocalDate periodEnd,
        boolean passed,
        int findingsCount,
        List<PreKraFinding> findings
) {
}
