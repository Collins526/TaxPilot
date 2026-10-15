package com.taxpilot.dto;

import java.time.LocalDate;

import com.taxpilot.domain.tax.FilingStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FilingStatusRequest(
        @NotBlank(message = "Tax type is required")
        @Size(max = 64, message = "Tax type must be 64 characters or fewer")
        String taxType,
        @NotNull(message = "Due date is required") LocalDate dueDate,
        @NotNull(message = "Filing status is required") FilingStatus filingStatus
) {
}
