package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import com.taxpilot.service.AdminBootstrapService.GeneratedCredentials;

class AdminBootstrapRunnerTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final AdminBootstrapService bootstrapService = org.mockito.Mockito.mock(AdminBootstrapService.class);
    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner(userRepository, bootstrapService);
    }

    @Test
    void skipsBootstrapWhenAdminAlreadyExistsWithoutCallingCreationService() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(true);

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));

        verify(bootstrapService, never()).createIfMissing();
    }

    @Test
    void delegatesAdminCreationWhenNoAdminExists() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false);
        when(bootstrapService.createIfMissing())
                .thenReturn(java.util.Optional.of(new GeneratedCredentials("admin@taxpilot.local", "temporary")));

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));

        verify(bootstrapService).createIfMissing();
    }

    @Test
    void suppressesUniqueConstraintRaceOnlyWhenAnotherAdminNowExists() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false, true);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(bootstrapService).createIfMissing();

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void rethrowsUniqueConstraintFailureWhenNoAdminWasCreated() {
        when(userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN))
                .thenReturn(false, false);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("unique constraint");
        org.mockito.Mockito.doThrow(failure).when(bootstrapService).createIfMissing();

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
    }
}
