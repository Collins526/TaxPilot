package com.taxpilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.taxpilot.domain.user.Role;
import com.taxpilot.repository.UserRepository;
import com.taxpilot.service.AdminBootstrapService;
import com.taxpilot.service.AdminBootstrapService.GeneratedCredentials;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapRunner(UserRepository userRepository, AdminBootstrapService adminBootstrapService) {
        this.userRepository = userRepository;
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (hasSystemAdmin()) {
            LOGGER.info("System administrator already exists; bootstrap skipped");
            return;
        }

        try {
            adminBootstrapService.createIfMissing().ifPresent(this::logGeneratedCredentials);
        } catch (DataIntegrityViolationException exception) {
            if (hasSystemAdmin()) {
                LOGGER.info("System administrator was created by another application instance; bootstrap skipped");
                return;
            }
            throw exception;
        }
    }

    private boolean hasSystemAdmin() {
        return userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN);
    }

    private void logGeneratedCredentials(GeneratedCredentials credentials) {
        LOGGER.warn("A system administrator was created. Retrieve these one-time credentials now; "
                        + "application logs expose them to anyone with log access. Store them securely.");
        LOGGER.warn("System administrator login email: {}", credentials.email());
        LOGGER.warn("System administrator temporary password: {}", credentials.password());
    }
}
