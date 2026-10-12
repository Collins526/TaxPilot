package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.taxpilot.service.JwtService;
import com.taxpilot.service.TokenRevocationService;

class TokenRevocationServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JwtService jwtService;
    private TokenRevocationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        jwtService = Mockito.mock(JwtService.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new TokenRevocationService(redisTemplate, jwtService);
    }

    @Test
    void revokeStoresHashedKeyWithRemainingTtl() {
        Mockito.when(jwtService.remainingValidityMillis("token-value")).thenReturn(120_000L);

        service.revoke("token-value");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(valueOperations).set(keyCaptor.capture(), eq("revoked"), eq(Duration.ofMillis(120_000L)));
        assertTrue(keyCaptor.getValue().startsWith("taxpilot:revoked:"));
        assertFalse(keyCaptor.getValue().contains("token-value"));
    }

    @Test
    void revokeSkipsExpiredToken() {
        Mockito.when(jwtService.remainingValidityMillis("expired")).thenReturn(0L);

        service.revoke("expired");

        Mockito.verify(valueOperations, Mockito.never()).set(anyString(), anyString(), Mockito.any(Duration.class));
    }

    @Test
    void isRevokedReturnsTrueWhenKeyExists() {
        Mockito.when(redisTemplate.hasKey(anyString())).thenReturn(Boolean.TRUE);

        assertTrue(service.isRevoked("token-value"));
    }

    @Test
    void isRevokedFailsOpenWhenRedisUnavailable() {
        Mockito.when(redisTemplate.hasKey(anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("down"));

        assertFalse(service.isRevoked("token-value"));
    }

    @Test
    void blankTokenIsNeverRevoked() {
        assertFalse(service.isRevoked("  "));
        service.revoke(null);
        Mockito.verify(valueOperations, Mockito.never()).set(anyString(), anyString(), Mockito.any(Duration.class));
    }
}
