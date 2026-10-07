package com.taxpilot.dto;

import java.util.UUID;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String firstName,
        String lastName,
        String email
) {
}
