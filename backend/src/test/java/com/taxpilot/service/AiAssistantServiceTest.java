package com.taxpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

class AiAssistantServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unconfiguredServiceRejectsRequests() {
        AiAssistantService service = new AiAssistantService("https://api.openai.com/v1", "", "gpt-4o-mini", objectMapper);

        assertFalse(service.isConfigured());
        assertThrows(ResponseStatusException.class, () -> service.ask("context", "question"));
    }

    @Test
    void configuredWhenKeyPresent() {
        AiAssistantService service = new AiAssistantService("https://api.openai.com/v1", "sk-test", "gpt-4o-mini", objectMapper);

        assertTrue(service.isConfigured());
        assertEquals("gpt-4o-mini", service.model());
        assertTrue(service.disclaimer().contains("deterministic engine"));
    }

    @Test
    void extractsAssistantContent() {
        AiAssistantService service = new AiAssistantService("https://api.openai.com/v1", "sk-test", "gpt-4o-mini", objectMapper);
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"  VAT is filed monthly.  \"}}]}";

        assertEquals("VAT is filed monthly.", service.extractContent(body));
    }

    @Test
    void rejectsResponseWithoutChoices() {
        AiAssistantService service = new AiAssistantService("https://api.openai.com/v1", "sk-test", "gpt-4o-mini", objectMapper);

        assertThrows(ResponseStatusException.class, () -> service.extractContent("{\"choices\":[]}"));
    }
}
