package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.taxpilot.storage.CloudinaryStorageService;

import tools.jackson.databind.ObjectMapper;

class CloudinarySignatureTest {

    private final CloudinaryStorageService service =
            new CloudinaryStorageService("cloud", "key", "secret", new ObjectMapper());

    @Test
    void signatureIsStableRegardlessOfInsertionOrder() {
        Map<String, String> first = new TreeMap<>();
        first.put("folder", "taxpilot");
        first.put("timestamp", "1700000000");

        Map<String, String> second = new LinkedHashMap<>();
        second.put("timestamp", "1700000000");
        second.put("folder", "taxpilot");

        assertEquals(service.sign(first), service.sign(second));
    }

    @Test
    void configuredWhenAllCredentialsPresent() {
        assertTrue(service.isConfigured());
        assertFalse(new CloudinaryStorageService("", "key", "secret", new ObjectMapper()).isConfigured());
    }
}
