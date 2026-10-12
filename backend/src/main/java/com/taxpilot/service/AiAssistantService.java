package com.taxpilot.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiAssistantService {

    static final String SYSTEM_PROMPT = """
            You are TaxPilot's assistant for Kenyan SMEs.
            You explain tax concepts, obligations, deadlines, and general next steps in plain language.

            HARD RULES:
            1. Never calculate, estimate, or guess any tax amount. Direct the user to the app's deterministic
               tax engine for any figure.
            2. Never invent tax rules, rates, thresholds, due dates, or rule versions. Only refer to the
               business context supplied to you. If something is not in that context, say it is not available.
            3. Do not claim to be KRA or to speak for KRA, and do not guarantee compliance outcomes.
            4. Keep answers concise, practical, and factual. Recommend a licensed tax professional for decisions.
            """;

    private static final String DISCLAIMER =
            "This is general guidance only, not tax advice or a calculation. TaxPilot's deterministic engine is "
                    + "the only source of amounts. Consult a qualified tax professional before acting.";

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiAssistantService(@Value("${assistant.base-url:https://api.openai.com/v1}") String baseUrl,
                              @Value("${assistant.api-key:}") String apiKey,
                              @Value("${assistant.model:gpt-4o-mini}") String model,
                              ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    public String disclaimer() {
        return DISCLAIMER;
    }

    public String ask(String businessContext, String question) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant is not configured");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "system", "content", "Business context:\n" + businessContext));
        messages.add(Map.of("role", "user", "content", question));

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", messages
        );

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not build assistant request", exception);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI assistant could not be reached");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI assistant request was interrupted");
        }

        if (response.statusCode() / 100 != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI assistant returned status " + response.statusCode());
        }
        return extractContent(response.body());
    }

    @SuppressWarnings("unchecked")
    String extractContent(String responseBody) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI assistant returned no answer");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message == null ? null : message.get("content");
            if (content == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI assistant returned an empty answer");
            }
            return content.toString().trim();
        } catch (JacksonException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI assistant returned an unreadable response");
        }
    }
}
