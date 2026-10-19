package com.taxpilot.dto;

import java.util.UUID;
import java.util.List;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        List<String> roles
) {
}
