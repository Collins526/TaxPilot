package com.taxpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

class AdminPasswordResetServiceTest {

    private static final UUID REQUEST_ID = UUID.fromString("e98fffc4-3122-4eb6-9bb3-82bd01bc3665");
    private static final String RESET_SQL =
            "INSERT INTO admin_password_reset_requests (request_id) VALUES (?) ON CONFLICT DO NOTHING";

    private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private AdminPasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new AdminPasswordResetService(jdbcTemplate, userRepository, passwordEncoder);
    }

    @Test
    void generatesAndHashesPasswordForExistingAdmin() {
        User admin = User.builder()
                .email("admin@taxpilot.com")
                .roles(Set.of(Role.ADMIN))
                .passwordHash("old-hash")
                .build();
        when(jdbcTemplate.update(RESET_SQL, REQUEST_ID)).thenReturn(1);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        var credentials = service.resetOnce(REQUEST_ID).orElseThrow();

        assertEquals("admin@taxpilot.com", credentials.email());
        assertTrue(credentials.password().length() >= 40);
        assertEquals("new-hash", admin.getPasswordHash());
        verify(passwordEncoder).encode(credentials.password());
        verify(userRepository).saveAndFlush(admin);
    }

    @Test
    void movesGeneratedAdminToStandardEmailWhileResetting() {
        User generatedAdmin = User.builder()
                .email("system-admin-0123456789abcdef0123456789abcdef@taxpilot.local")
                .roles(Set.of(Role.ADMIN))
                .passwordHash("old-hash")
                .build();
        when(jdbcTemplate.update(RESET_SQL, REQUEST_ID)).thenReturn(1);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailStartingWith("system-admin-")).thenReturn(List.of(generatedAdmin));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        var credentials = service.resetOnce(REQUEST_ID).orElseThrow();

        assertEquals("admin@taxpilot.com", generatedAdmin.getEmail());
        assertEquals("admin@taxpilot.com", credentials.email());
        assertEquals("new-hash", generatedAdmin.getPasswordHash());
        verify(userRepository).saveAndFlush(generatedAdmin);
    }

    @Test
    void doesNotResetAgainForPreviouslyUsedRequestId() {
        when(jdbcTemplate.update(RESET_SQL, REQUEST_ID)).thenReturn(0);

        assertFalse(service.resetOnce(REQUEST_ID).isPresent());

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changesExistingAdminPasswordAfterVerifyingCurrentPassword() {
        User admin = User.builder()
                .email("admin@taxpilot.com")
                .roles(Set.of(Role.ADMIN))
                .passwordHash("old-hash")
                .build();
        when(jdbcTemplate.update(RESET_SQL, REQUEST_ID)).thenReturn(1);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        assertTrue(service.changePasswordOnce(REQUEST_ID, "current-password", "new-password"));

        assertEquals("new-hash", admin.getPasswordHash());
        verify(userRepository).saveAndFlush(admin);
    }

    @Test
    void refusesPasswordChangeWhenCurrentPasswordDoesNotMatch() {
        User admin = User.builder()
                .email("admin@taxpilot.com")
                .roles(Set.of(Role.ADMIN))
                .passwordHash("old-hash")
                .build();
        when(jdbcTemplate.update(RESET_SQL, REQUEST_ID)).thenReturn(1);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.changePasswordOnce(REQUEST_ID, "wrong-password", "new-password"));

        assertEquals("old-hash", admin.getPasswordHash());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void refusesToResetWhenTargetEmailIsNotAnAdmin() {
        User regularUser = User.builder()
                .email("admin@taxpilot.com")
                .roles(Set.of(Role.USER))
                .passwordHash("unchanged-hash")
                .build();
        when(jdbcTemplate.update(RESET_SQL, REQUEST_ID)).thenReturn(1);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com")).thenReturn(Optional.of(regularUser));

        assertThrows(IllegalStateException.class, () -> service.resetOnce(REQUEST_ID));

        assertEquals("unchanged-hash", regularUser.getPasswordHash());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
