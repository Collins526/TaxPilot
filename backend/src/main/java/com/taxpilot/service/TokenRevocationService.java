package com.taxpilot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);
    private static final String KEY_PREFIX = "taxpilot:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public TokenRevocationService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        long ttlMillis = jwtService.remainingValidityMillis(token);
        if (ttlMillis <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(token), "revoked", Duration.ofMillis(ttlMillis));
        } catch (RuntimeException exception) {
            log.warn("Unable to record token revocation in Redis; continuing without blacklist entry", exception);
        }
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
        } catch (RuntimeException exception) {
            log.warn("Unable to check token revocation in Redis; treating token as active", exception);
            return false;
        }
    }

    private String key(String token) {
        return KEY_PREFIX + hash(token);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
