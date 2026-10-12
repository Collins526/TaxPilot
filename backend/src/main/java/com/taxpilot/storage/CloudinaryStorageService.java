package com.taxpilot.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class CloudinaryStorageService implements DocumentStorageService {

    private static final String PROVIDER = "cloudinary";

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CloudinaryStorageService(@Value("${cloudinary.cloud-name:}") String cloudName,
                                    @Value("${cloudinary.api-key:}") String apiKey,
                                    @Value("${cloudinary.api-secret:}") String apiSecret,
                                    ObjectMapper objectMapper) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean isConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    @Override
    public StoredObject store(byte[] content, String fileName, String contentType, String folder) {
        if (!isConfigured()) {
            throw new IllegalStateException("Document storage is not configured");
        }

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        Map<String, String> signedParams = new TreeMap<>();
        signedParams.put("folder", folder);
        signedParams.put("public_id", fileName);
        signedParams.put("timestamp", timestamp);
        String signature = sign(signedParams);

        String boundary = "----TaxPilotBoundary" + System.nanoTime();
        byte[] body = buildMultipart(boundary, content, fileName, contentType, timestamp, folder, signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Cloudinary upload failed with status " + response.statusCode());
            }
            Map<?, ?> payload = objectMapper.readValue(response.body(), Map.class);
            Object url = payload.get("secure_url");
            Object publicId = payload.get("public_id");
            if (url == null || publicId == null) {
                throw new IllegalStateException("Cloudinary response did not include a secure URL");
            }
            return new StoredObject(PROVIDER, url.toString(), publicId.toString());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cloudinary returned an unreadable response", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Cloudinary upload could not be completed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cloudinary upload was interrupted", exception);
        }
    }

    public String sign(Map<String, String> signedParams) {
        StringBuilder toSign = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<>(signedParams).entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (!toSign.isEmpty()) {
                toSign.append('&');
            }
            toSign.append(entry.getKey()).append('=').append(entry.getValue());
        }
        toSign.append(apiSecret);
        return sha1Hex(toSign.toString());
    }

    private byte[] buildMultipart(String boundary,
                                  byte[] content,
                                  String fileName,
                                  String contentType,
                                  String timestamp,
                                  String folder,
                                  String signature) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("api_key", apiKey);
        fields.put("timestamp", timestamp);
        fields.put("folder", folder);
        fields.put("public_id", fileName);
        fields.put("signature", signature);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                out.write((field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not assemble upload payload", exception);
        }
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 not available", exception);
        }
    }
}
