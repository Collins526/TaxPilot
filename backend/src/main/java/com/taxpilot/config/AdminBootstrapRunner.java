package com.taxpilot.config;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.taxpilot.domain.user.Role;
import com.taxpilot.repository.UserRepository;
import com.taxpilot.service.AdminBootstrapService;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final AdminBootstrapService adminBootstrapService;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String password;

    public AdminBootstrapRunner(UserRepository userRepository,
                                AdminBootstrapService adminBootstrapService,
                                @Value("${app.bootstrap-admin.first-name:}") String firstName,
                                @Value("${app.bootstrap-admin.last-name:}") String lastName,
                                @Value("${app.bootstrap-admin.email:}") String email,
                                @Value("${app.bootstrap-admin.phone:}") String phone,
                                @Value("${app.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.adminBootstrapService = adminBootstrapService;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (hasSystemAdmin()) {
            LOGGER.info("System administrator already exists; bootstrap skipped");
            return;
        }

        validateConfiguration();
        try {
            adminBootstrapService.createIfMissing(firstName, lastName, email, phone, password);
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

    private void validateConfiguration() {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()
                || phone.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "No system administrator exists. Configure BOOTSTRAP_ADMIN_FIRST_NAME, "
                            + "BOOTSTRAP_ADMIN_LAST_NAME, BOOTSTRAP_ADMIN_EMAIL, "
                            + "BOOTSTRAP_ADMIN_PHONE, and BOOTSTRAP_ADMIN_PASSWORD before startup.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL must be a valid email address.");
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PHONE must be a valid phone number.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be no more than 72 UTF-8 bytes.");
        }
    }
}
