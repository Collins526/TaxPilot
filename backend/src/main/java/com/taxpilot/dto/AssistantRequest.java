package com.taxpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantRequest(
        @NotBlank(message = "A question is required")
        @Size(max = 2000, message = "Question must be 2000 characters or fewer")
        String message
) {
}
