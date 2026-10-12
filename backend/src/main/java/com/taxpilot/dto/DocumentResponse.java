package com.taxpilot.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        String category,
        String storageProvider,
        String url,
        LocalDateTime uploadedAt
) {
}
