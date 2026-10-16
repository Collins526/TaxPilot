package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataIntegrityViolationException;

import com.taxpilot.config.AdminBootstrapRunner;
import com.taxpilot.domain.user.Role;
import com.taxpilot.repository.UserRepository;
import com.taxpilot.service.AdminBootstrapService;

class AdminBootstrapRunnerTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final AdminBootstrapService bootstrapService = org.mockito.Mockito.mock(AdminBootstrapService.class);
    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner(userRepository, bootstrapService,
                "System", "Administrator", "admin@example.test", "+254700000000", "StrongTestPass123");
    }

    @Test
    void skipsBootstrapWhenAdminAlreadyExistsWithoutCallingCreationService() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(true);

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));

        verify(bootstrapService, never()).createIfMissing(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void requiresValidBootstrapConfigurationWhenNoAdminExists() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        runner = new AdminBootstrapRunner(userRepository, bootstrapService, "", "", "", "", "");

        assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));

        verify(bootstrapService, never()).createIfMissing(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsInvalidEmailAndTooShortBootstrapPassword() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        runner = new AdminBootstrapRunner(userRepository, bootstrapService,
                "System", "Administrator", "not-an-email", "+254700000000", "short");

        assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));

        runner = new AdminBootstrapRunner(userRepository, bootstrapService,
                "System", "Administrator", "admin@example.test", "+254700000000", "short");
        assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));

        verify(bootstrapService, never()).createIfMissing(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void suppressesUniqueConstraintRaceOnlyWhenAnotherAdminNowExists() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false, true);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(bootstrapService).createIfMissing(
                        "System", "Administrator", "admin@example.test", "+254700000000", "StrongTestPass123");

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void rethrowsUniqueConstraintFailureWhenNoAdminWasCreated() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false, false);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("unique constraint");
        org.mockito.Mockito.doThrow(failure).when(bootstrapService).createIfMissing(
                "System", "Administrator", "admin@example.test", "+254700000000", "StrongTestPass123");

        assertThrows(DataIntegrityViolationException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
    }
}
