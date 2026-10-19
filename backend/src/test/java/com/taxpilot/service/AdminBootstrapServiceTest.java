package com.taxpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

class AdminBootstrapServiceTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new AdminBootstrapService(userRepository, passwordEncoder);
    }

    @Test
    void createsEnabledAdminWithOnlyAdminRoleAndHashedGeneratedPassword() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$hashed");

        var credentials = service.createIfMissing().orElseThrow();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User created = userCaptor.getValue();
        assertEquals("System", created.getFirstName());
        assertEquals("Administrator", created.getLastName());
        assertEquals("admin@taxpilot.com", credentials.email());
        assertEquals(credentials.email(), created.getEmail());
        assertEquals("Not provided", created.getPhone());
        assertTrue(credentials.password().length() >= 40);
        assertEquals("$2a$hashed", created.getPasswordHash());
        assertEquals(EnumSet.of(Role.ADMIN), created.getRoles());
        assertTrue(created.isEnabled());
        verify(passwordEncoder).encode(credentials.password());
    }

    @Test
    void skipsCreationIfAdminAlreadyExists() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(true);

        assertTrue(service.createIfMissing().isEmpty());

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void refusesToPromoteAnExistingNonAdminAtTheStandardAdminEmail() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@taxpilot.com"))
                .thenReturn(Optional.of(User.builder().email("admin@taxpilot.com").build()));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, service::createIfMissing);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }
}
