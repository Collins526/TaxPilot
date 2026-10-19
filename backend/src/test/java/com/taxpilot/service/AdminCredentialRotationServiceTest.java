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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

class AdminCredentialRotationServiceTest {

    private static final String TARGET_EMAIL = "admin@taxpilot.com";
    private static final String NEW_PASSWORD = "Use-a-strong-local-secret-123";

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private AdminCredentialRotationService service;

    @BeforeEach
    void setUp() {
        service = new AdminCredentialRotationService(userRepository, passwordEncoder);
        when(userRepository.findByEmailIgnoreCase(TARGET_EMAIL)).thenReturn(Optional.empty());
    }

    @Test
    void rotatesOnlyGeneratedAdminAndHashesTheNewPassword() {
        User generatedAdmin = generatedAdmin();
        when(userRepository.findByEmailStartingWith("system-admin-")).thenReturn(List.of(generatedAdmin));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("$2a$rotated-hash");

        assertTrue(service.rotateGeneratedAdmin(TARGET_EMAIL, NEW_PASSWORD));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(TARGET_EMAIL, userCaptor.getValue().getEmail());
        assertEquals("$2a$rotated-hash", userCaptor.getValue().getPasswordHash());
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    void doesNotRehashWhenCredentialsAlreadyMatch() {
        User admin = User.builder()
                .email(TARGET_EMAIL)
                .roles(Set.of(Role.ADMIN))
                .passwordHash("existing-hash")
                .build();
        when(userRepository.findByEmailIgnoreCase(TARGET_EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(NEW_PASSWORD, "existing-hash")).thenReturn(true);

        assertFalse(service.rotateGeneratedAdmin(TARGET_EMAIL, NEW_PASSWORD));

        verify(userRepository, never()).findByEmailStartingWith(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void refusesToOverwriteAnExistingAccountAtTargetEmail() {
        User existingUser = User.builder()
                .email(TARGET_EMAIL)
                .roles(Set.of(Role.USER))
                .passwordHash("unrelated-hash")
                .build();
        when(userRepository.findByEmailIgnoreCase(TARGET_EMAIL)).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalStateException.class,
                () -> service.rotateGeneratedAdmin(TARGET_EMAIL, NEW_PASSWORD));

        verify(userRepository, never()).findByEmailStartingWith(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void refusesToChooseBetweenMultipleGeneratedAdmins() {
        when(userRepository.findByEmailStartingWith("system-admin-"))
                .thenReturn(List.of(generatedAdmin(), generatedAdmin()));

        assertThrows(IllegalStateException.class,
                () -> service.rotateGeneratedAdmin(TARGET_EMAIL, NEW_PASSWORD));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    private User generatedAdmin() {
        return User.builder()
                .email("system-admin-0123456789abcdef0123456789abcdef@taxpilot.local")
                .roles(Set.of(Role.ADMIN))
                .passwordHash("initial-hash")
                .build();
    }
}
