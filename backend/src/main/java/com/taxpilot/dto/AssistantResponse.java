package com.taxpilot.dto;

public record AssistantResponse(
        String answer,
        String disclaimer,
        String model
) {
}
