package com.taxpilot.storage;

public record StoredObject(
        String provider,
        String url,
        String publicId
) {
}
