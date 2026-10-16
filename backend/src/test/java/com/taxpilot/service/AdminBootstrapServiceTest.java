package com.taxpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void createsEnabledAdminWithOnlyAdminRoleAndHashedPassword() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongTestPass123")).thenReturn("$2a$hashed");

        service.createIfMissing(" System ", " Administrator ", "Admin@Example.Test ",
                " +254700000000 ", "StrongTestPass123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User created = userCaptor.getValue();
        assertEquals("System", created.getFirstName());
        assertEquals("Administrator", created.getLastName());
        assertEquals("admin@example.test", created.getEmail());
        assertEquals("+254700000000", created.getPhone());
        assertEquals("$2a$hashed", created.getPasswordHash());
        assertEquals(EnumSet.of(Role.ADMIN), created.getRoles());
        assertTrue(created.isEnabled());
        verify(passwordEncoder).encode("StrongTestPass123");
    }

    @Test
    void skipsCreationIfAdminAlreadyExists() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(true);

        service.createIfMissing("System", "Administrator", "admin@example.test",
                "+254700000000", "StrongTestPass123");

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void refusesToPromoteAnExistingNonAdminAccount() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.test"))
                .thenReturn(Optional.of(User.builder().email("admin@example.test").build()));

        assertThrows(IllegalStateException.class,
                () -> service.createIfMissing("System", "Administrator", "admin@example.test",
                        "+254700000000", "StrongTestPass123"));

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }
}
