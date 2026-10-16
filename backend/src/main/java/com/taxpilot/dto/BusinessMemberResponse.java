package com.taxpilot.dto;

import java.util.UUID;

public record BusinessMemberResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
